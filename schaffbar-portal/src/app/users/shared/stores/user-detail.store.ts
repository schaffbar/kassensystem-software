import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { mutation } from '../../../shared/state/mutation';
import { AssignRfidTagCommand, UnassignRfidTagCommand } from '../models/rfid-tag-assignment.model';
import {
  toRequestRfidTagAssignmentRequest,
  toRfidTagAssignment,
  toUnassignRfidTagRequest,
} from '../models/rfid-tag-assignment.mapper';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../models/tool-certification.model';
import { toToolCertification } from '../models/tool-certification.mapper';
import { toTool } from '../models/tool.mapper';
import { ToolsApiService } from '../../../shared/api/tools/tools.api-service';
import { UpdateUserAddressCommand, UpdateUserCommand, UpdateUserContactCommand } from '../models/user.model';
import { CloseWorkshopSessionCommand, hasRunningUsage } from '../models/workshop-session.model';
import { toWorkshopSession } from '../models/workshop-session.mapper';
import { RfidTagAssignmentsApiService } from '../../../shared/api/rfid-tag-assignments/rfid-tag-assignments.api-service';
import { ToolCertificationsApiService } from '../../../shared/api/tool-certifications/tool-certifications.api-service';
import { UsersService } from '../services/users.service';
import { WorkshopSessionsApiService } from '../../../shared/api/workshop-sessions/workshop-sessions.api-service';

interface UserDetailState {
  userId: string | null;
}

export const UserDetailStore = signalStore(
  withState<UserDetailState>({ userId: null }),
  withProps((store) => {
    const usersService = inject(UsersService);
    const rfidTagAssignmentsApi = inject(RfidTagAssignmentsApiService);
    const workshopSessionsApi = inject(WorkshopSessionsApiService);
    const toolCertificationsApi = inject(ToolCertificationsApiService);
    return {
      // services for the mutations
      _usersService: usersService,
      _rfidTagAssignmentsApi: rfidTagAssignmentsApi,
      _workshopSessionsApi: workshopSessionsApi,
      _toolCertificationsApi: toolCertificationsApi,
      // resources for the queries
      _userResource: usersService.userResource(store.userId),
      _assignmentResource: rfidTagAssignmentsApi.currentRfidTagAssignmentResource(store.userId),
      _openSessionResource: workshopSessionsApi.openSessionResource(store.userId),
      _certificationsResource: toolCertificationsApi.certificationsByCustomerResource(store.userId),
      _toolsResource: inject(ToolsApiService).toolsResource(),
    };
  }),
  withComputed(
    ({ _userResource, _assignmentResource, _openSessionResource, _certificationsResource, _toolsResource }) => {
      const resources = [
        _userResource,
        _assignmentResource,
        _openSessionResource,
        _certificationsResource,
        _toolsResource,
      ];
      return {
        // hasValue() stays true while reloading, so the page keeps showing the data instead of flickering
        user: computed(() => (_userResource.hasValue() ? _userResource.value() : null)),
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
        // loading and error states are combined from all resources
        isLoading: computed(() => resources.some((resource) => resource.isLoading())),
        hasLoadError: computed(() => resources.some((resource) => !!resource.error())),
      };
    },
  ),
  withComputed(({ user, openSession }) => ({
    userName: computed(() => (user() ? user()?.firstName + ' ' + user()?.lastName : 'N/A')),
    slots: computed(() => openSession()?.workshopUsages ?? []),
    toolUsageSummaries: computed(() => openSession()?.toolUsageSummaries ?? []),
    totalWorkshopDurationInMinutes: computed(() => openSession()?.totalWorkshopDurationInMinutes ?? 0),
    totalWorkshopUnits: computed(() => openSession()?.totalWorkshopUnits ?? 0),
    hasRunningUsage: computed(() => {
      const session = openSession();
      return session ? hasRunningUsage(session) : false;
    }),
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
    },
  })),
  // mutations of the user – reload the user on success
  withMethods((store) => ({
    updateUser: mutation<UpdateUserCommand>({
      request: (command) => store._usersService.updateUser(command),
      onSuccess: () => store._userResource.reload(),
    }),
    updateUserContact: mutation<UpdateUserContactCommand>({
      request: (command) => store._usersService.updateUserContact(command),
      onSuccess: () => store._userResource.reload(),
    }),
    updateUserAddress: mutation<UpdateUserAddressCommand>({
      request: (command) => store._usersService.updateUserAddress(command),
      onSuccess: () => store._userResource.reload(),
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
    closeSession: mutation<CloseWorkshopSessionCommand>({
      request: (command) => store._workshopSessionsApi.closeWorkshopSession(command.customerId),
      onSuccess: () => store._openSessionResource.reload(),
    }),
  })),
  // mutations of the certifications – reload the certifications on success
  withMethods((store) => ({
    pauseCertification: mutation<PauseToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.pause(command.customerId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    reactivateCertification: mutation<ReactivateToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.reactivate(command.customerId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    revokeCertification: mutation<RevokeToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.revoke(command.customerId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
  })),
);
