import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { mutation } from '../../../shared/state/mutation';
import { AssignRfidTagCommand, UnassignRfidTagCommand } from '../../shared/models/rfid-tag-assignment.model';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../shared/models/tool-certification.model';
import { UpdateUserAddressCommand, UpdateUserCommand, UpdateUserContactCommand } from '../../shared/models/user.model';
import { CloseWorkshopSessionCommand } from '../../shared/models/workshop-session.model';
import { RfidTagAssignmentService } from '../../shared/services/rfid-tag-assignment.service';
import { ToolCertificationService } from '../../shared/services/tool-certification.service';
import { UsersService } from '../../shared/services/users.service';
import { WorkshopSessionService } from '../../shared/services/workshop-session.service';

interface UserDetailState {
  userId: string | null;
}

export const UserDetailStore = signalStore(
  withState<UserDetailState>({ userId: null }),
  withProps((store) => {
    const usersService = inject(UsersService);
    const assignmentService = inject(RfidTagAssignmentService);
    const workshopSessionService = inject(WorkshopSessionService);
    const toolCertificationService = inject(ToolCertificationService);
    return {
      // services for the mutations
      _usersService: usersService,
      _assignmentService: assignmentService,
      _workshopSessionService: workshopSessionService,
      _toolCertificationService: toolCertificationService,
      // resources for the queries
      _userResource: usersService.userResource(store.userId),
      _assignmentResource: assignmentService.rfidTagAssignmentResource(store.userId),
      _openSessionResource: workshopSessionService.openSessionResource(store.userId),
      _certificationsResource: toolCertificationService.certificationsByUserResource(store.userId),
    };
  }),
  withComputed(({ _userResource, _assignmentResource, _openSessionResource, _certificationsResource }) => {
    const resources = [_userResource, _assignmentResource, _openSessionResource, _certificationsResource];
    return {
      // hasValue() stays true while reloading, so the page keeps showing the data instead of flickering
      user: computed(() => (_userResource.hasValue() ? _userResource.value() : null)),
      rfidTagAssignment: computed(() => (_assignmentResource.hasValue() ? _assignmentResource.value() : null)),
      openSession: computed(() => (_openSessionResource.hasValue() ? _openSessionResource.value() : null)),
      toolCertifications: computed(() => (_certificationsResource.hasValue() ? _certificationsResource.value() : [])),
      // loading and error states are combined from all resources
      isLoading: computed(() => resources.some((resource) => resource.isLoading())),
      hasLoadError: computed(() => resources.some((resource) => !!resource.error())),
    };
  }),
  withComputed(({ user, openSession }) => ({
    userName: computed(() => (user() ? user()?.firstName + ' ' + user()?.lastName : 'N/A')),
    slots: computed(() => openSession()?.workshopUsages ?? []),
    toolUsageSummaries: computed(() => openSession()?.toolUsageSummaries ?? []),
  })),
  withComputed(({ slots }) => ({
    // a running usage (no exit time yet) has no duration and is not counted
    totalWorkshopTimeInMinutes: computed(() => slots().reduce((sum, usage) => sum + (usage.durationInMinutes ?? 0), 0)),
    totalWorkshopUnitsUsed: computed(() => slots().reduce((sum, usage) => sum + (usage.unitsUsed ?? 0), 0)),
  })),
  withMethods((store) => ({
    setUserId: signalMethod<string>((userId) => {
      patchState(store, { userId });
    }),
    reloadUser(): void {
      store._userResource.reload();
      store._assignmentResource.reload();
      store._openSessionResource.reload();
      store._certificationsResource.reload();
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
      request: (command) => store._assignmentService.assignRfidTag(command),
      onSuccess: () => store._assignmentResource.reload(),
    }),
    unassignRfidTag: mutation<UnassignRfidTagCommand>({
      request: (command) => store._assignmentService.unassignRfidTag(command),
      onSuccess: () => store._assignmentResource.reload(),
    }),
  })),
  // mutations of the open session – reload the session on success
  withMethods((store) => ({
    closeSession: mutation<CloseWorkshopSessionCommand>({
      request: (command) => store._workshopSessionService.closeWorkshopSession(command),
      onSuccess: () => store._openSessionResource.reload(),
    }),
  })),
  // mutations of the certifications – reload the certifications on success
  withMethods((store) => ({
    pauseCertification: mutation<PauseToolCertificationCommand>({
      request: (command) => store._toolCertificationService.pause(command),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    reactivateCertification: mutation<ReactivateToolCertificationCommand>({
      request: (command) => store._toolCertificationService.reactivate(command),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    revokeCertification: mutation<RevokeToolCertificationCommand>({
      request: (command) => store._toolCertificationService.revoke(command),
      onSuccess: () => store._certificationsResource.reload(),
    }),
  })),
);
