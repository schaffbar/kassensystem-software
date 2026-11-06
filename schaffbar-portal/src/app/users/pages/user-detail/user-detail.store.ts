import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { mutation } from '../../../shared/state/mutation';
import { AssignRfidTagCommand, UnassignRfidTagCommand } from '../../rfid-tag-assignment.model';
import {
  toRequestRfidTagAssignmentRequest,
  toRfidTagAssignment,
  toUnassignRfidTagRequest,
} from '../../rfid-tag-assignment.mapper';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../tool-certification.model';
import { toToolCertification } from '../../tool-certification.mapper';
import { toTool } from '../../tool.mapper';
import { ToolsApiService } from '../../../shared/api/tools/tools.api-service';
import {
  toUpdateCustomerAddressRequest,
  toUpdateCustomerContactRequest,
  toUpdateCustomerRequest,
  toUser,
} from '../../user.mapper';
import { UpdateUserAddressCommand, UpdateUserCommand, UpdateUserContactCommand } from '../../user.model';
import { CloseWorkshopSessionCommand, hasRunningUsage } from '../../workshop-session.model';
import { toWorkshopSession } from '../../workshop-session.mapper';
import { RfidTagAssignmentsApiService } from '../../../shared/api/rfid-tag-assignments/rfid-tag-assignments.api-service';
import { ToolCertificationsApiService } from '../../../shared/api/tool-certifications/tool-certifications.api-service';
import { CustomersApiService } from '../../../shared/api/customers/customers.api-service';
import { WorkshopSessionsApiService } from '../../../shared/api/workshop-sessions/workshop-sessions.api-service';

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
      _userResource: customersApi.customerResource(store.userId),
      _assignmentResource: rfidTagAssignmentsApi.currentRfidTagAssignmentResource(store.userId),
      _openSessionResource: workshopSessionsApi.activeWorkshopSessionResource(store.userId),
      _certificationsResource: toolCertificationsApi.toolCertificationsByCustomerResource(store.userId),
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
        isLoading: computed(() => resources.some((resource) => resource.isLoading())),
        hasLoadError: computed(() => resources.some((resource) => !!resource.error())),
      };
    },
  ),
  withComputed(({ openSession }) => ({
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
      request: (command) => store._customersApi.updateCustomer(command.userId, toUpdateCustomerRequest(command)),
      onSuccess: () => store._userResource.reload(),
    }),
    updateUserContact: mutation<UpdateUserContactCommand>({
      request: (command) =>
        store._customersApi.updateCustomerContact(command.userId, toUpdateCustomerContactRequest(command)),
      onSuccess: () => store._userResource.reload(),
    }),
    updateUserAddress: mutation<UpdateUserAddressCommand>({
      request: (command) =>
        store._customersApi.updateCustomerAddress(command.userId, toUpdateCustomerAddressRequest(command)),
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
  })),
);
