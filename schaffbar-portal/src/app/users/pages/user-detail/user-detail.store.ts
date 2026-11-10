import { computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { concat, EMPTY, interval, Observable, pipe, switchMap, tap, toArray } from 'rxjs';

import { ROUTE } from '../../../app.routes';
import { CustomersApiService } from '../../../shared/api/customers/customers.api-service';
import { RfidTagAssignmentsApiService } from '../../../shared/api/rfid-tag-assignments/rfid-tag-assignments.api-service';
import { ToolCertificationsApiService } from '../../../shared/api/tool-certifications/tool-certifications.api-service';
import { ToolsApiService } from '../../../shared/api/tools/tools.api-service';
import { WorkshopSessionsApiService } from '../../../shared/api/workshop-sessions/workshop-sessions.api-service';
import { mutation } from '../../../shared/state/mutation';
import {
  toRequestRfidTagAssignmentRequest,
  toRfidTagAssignment,
  toUnassignRfidTagRequest,
} from '../../rfid-tag-assignment.mapper';
import { AssignRfidTagCommand, isWaitingForRfidTag, UnassignRfidTagCommand } from '../../rfid-tag-assignment.model';
import { toToolCertification } from '../../tool-certification.mapper';
import {
  DeleteToolCertificationCommand,
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../tool-certification.model';
import { toTool } from '../../tool.mapper';
import {
  toUpdateCustomerAddressRequest,
  toUpdateCustomerContactRequest,
  toUpdateCustomerRequest,
  toUser,
} from '../../user.mapper';
import {
  DeleteUserCommand,
  hasAddressChanged,
  hasContactChanged,
  hasPersonChanged,
  UpdateUserCommand,
  User,
} from '../../user.model';
import { toWorkshopSession } from '../../workshop-session.mapper';
import { canCloseWorkshopSession, CloseWorkshopSessionCommand } from '../../workshop-session.model';

// how often a requested tag is checked while it waits at the counter
const RFID_TAG_POLL_INTERVAL_MS = 3000;

interface UserDetailState {
  userId: string | null;
}

export const UserDetailStore = signalStore(
  withState<UserDetailState>({ userId: null }),
  withProps((store) => {
    const customersApi = inject(CustomersApiService);
    const rfidTagAssignmentsApi = inject(RfidTagAssignmentsApiService);
    const workshopSessionsApi = inject(WorkshopSessionsApiService);
    const toolCertificationsApi = inject(ToolCertificationsApiService);
    return {
      _customersApi: customersApi,
      _rfidTagAssignmentsApi: rfidTagAssignmentsApi,
      _workshopSessionsApi: workshopSessionsApi,
      _toolCertificationsApi: toolCertificationsApi,
      _router: inject(Router),
      _userResource: customersApi.customerResource(store.userId),
      _assignmentResource: rfidTagAssignmentsApi.currentRfidTagAssignmentResource(store.userId),
      _openSessionResource: workshopSessionsApi.activeWorkshopSessionResource(store.userId),
      _certificationsResource: toolCertificationsApi.toolCertificationsByCustomerResource(store.userId),
      // names and areas of the tools, names of the instructors
      _toolsResource: inject(ToolsApiService).toolsResource(),
      _usersResource: customersApi.customersResource(),
    };
  }),
  withComputed(
    ({
      _userResource,
      _assignmentResource,
      _openSessionResource,
      _certificationsResource,
      _toolsResource,
      _usersResource,
    }) => {
      const resources = [_userResource, _openSessionResource, _certificationsResource, _toolsResource, _usersResource];
      return {
        // hasValue() stays true while reloading, so the page keeps showing the data instead of flickering
        user: computed(() => (_userResource.hasValue() ? toUser(_userResource.value()) : null)),
        rfidTagAssignment: computed(() => {
          const assignment = _assignmentResource.hasValue() ? _assignmentResource.value() : null;
          return assignment ? toRfidTagAssignment(assignment) : null;
        }),
        openSession: computed(() => {
          const session = _openSessionResource.hasValue() ? _openSessionResource.value() : null;
          return session ? toWorkshopSession(session) : null;
        }),
        toolCertifications: computed(() =>
          _certificationsResource.hasValue() ? _certificationsResource.value().map(toToolCertification) : [],
        ),
        tools: computed(() => (_toolsResource.hasValue() ? _toolsResource.value().map(toTool) : [])),
        userNames: computed(
          () =>
            new Map(
              (_usersResource.hasValue() ? _usersResource.value() : []).map((user) => [
                user.id,
                `${user.firstName} ${user.lastName}`,
              ]),
            ),
        ),
        // only the first load of the assignment – it is reloaded every few seconds while a tag is requested, the
        // progress bar would flicker
        isLoading: computed(
          () => resources.some((resource) => resource.isLoading()) || _assignmentResource.status() === 'loading',
        ),
        hasLoadError: computed(() => [...resources, _assignmentResource].some((resource) => !!resource.error())),
      };
    },
  ),
  withComputed(({ openSession, rfidTagAssignment }) => ({
    canCloseWorkshopSession: computed(() => {
      const session = openSession();
      return !!session && canCloseWorkshopSession(session);
    }),
    // a user with an open session is not deleted – the session still has to be closed and paid
    canDeleteUser: computed(() => !openSession()),
    isWaitingForRfidTag: computed(() => isWaitingForRfidTag(rfidTagAssignment())),
  })),
  withMethods((store) => ({
    setUserId: signalMethod<string>((userId) => {
      patchState(store, { userId });
    }),
    reloadOpenSession(): void {
      store._openSessionResource.reload();
    },
    reloadUser(): void {
      store._userResource.reload();
      store._assignmentResource.reload();
      store._openSessionResource.reload();
      store._certificationsResource.reload();
      store._toolsResource.reload();
      store._usersResource.reload();
    },
    // reloads the assignment every few seconds while `waiting` is true – the page shows the tag as soon as it has been
    // held to the reader at the counter
    pollRfidTagAssignment: rxMethod<boolean>(
      pipe(
        switchMap((waiting) =>
          waiting ? interval(RFID_TAG_POLL_INTERVAL_MS).pipe(tap(() => store._assignmentResource.reload())) : EMPTY,
        ),
      ),
    ),
  })),
  // mutations of the user
  withMethods((store) => ({
    updateUser: mutation<UpdateUserCommand>({
      request: (command) => concat(...updateUserRequests(store, command)).pipe(toArray()),
      onSuccess: () => store._userResource.reload(),
    }),
    deleteUser: mutation<DeleteUserCommand>({
      request: (command) => store._customersApi.deleteCustomer(command.userId),
      onSuccess: () => void store._router.navigate([ROUTE.USERS]),
    }),
  })),
  // mutations of the RFID tag assignment – reload the assignment on success
  withMethods((store) => ({
    assignRfidTag: mutation<AssignRfidTagCommand>({
      request: (command) =>
        store._rfidTagAssignmentsApi.requestRfidTagAssignment(toRequestRfidTagAssignmentRequest(command)),
      onSuccess: () => store._assignmentResource.reload(),
    }),
    unassignRfidTag: mutation<UnassignRfidTagCommand>({
      request: (command) => store._rfidTagAssignmentsApi.unassignRfidTag(toUnassignRfidTagRequest(command)),
      onSuccess: () => store._assignmentResource.reload(),
    }),
  })),
  // mutations of the open session – reload the session on success
  withMethods((store) => ({
    closeWorkshopSession: mutation<CloseWorkshopSessionCommand>({
      request: (command) => store._workshopSessionsApi.closeWorkshopSession(command.userId),
      onSuccess: () => store._openSessionResource.reload(),
    }),
  })),
  // mutations of the certifications – reload the certifications on success
  withMethods((store) => ({
    pauseToolCertification: mutation<PauseToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.pauseToolCertification(command.userId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    reactivateToolCertification: mutation<ReactivateToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.reactivateToolCertification(command.userId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    revokeToolCertification: mutation<RevokeToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.revokeToolCertification(command.userId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    deleteToolCertification: mutation<DeleteToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.deleteToolCertification(command.userId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
  })),
);

// ----------------------------------------------------------------------------
// helper

// one request per changed part, one after the other – person, contact and address are three endpoints
function updateUserRequests(
  store: { user: () => User | null; _customersApi: CustomersApiService },
  command: UpdateUserCommand,
): Observable<void>[] {
  const user = store.user();
  const api = store._customersApi;
  const requests: Observable<void>[] = [];
  if (!user || hasPersonChanged(user, command)) {
    requests.push(api.updateCustomer(command.userId, toUpdateCustomerRequest(command)));
  }
  if (!user || hasContactChanged(user, command)) {
    requests.push(api.updateCustomerContact(command.userId, toUpdateCustomerContactRequest(command)));
  }
  if (!user || hasAddressChanged(user, command)) {
    requests.push(api.updateCustomerAddress(command.userId, toUpdateCustomerAddressRequest(command)));
  }
  return requests;
}
