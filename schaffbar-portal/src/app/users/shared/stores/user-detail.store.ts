import { computed, inject } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, pipe, tap } from 'rxjs';

import { setError, setFulfilled, setPending, withRequestStatus } from '../../../shared/state/request-status.feature';
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
  withRequestStatus(),
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
    totalWorkshopTimeInMinutes: computed(() => slots().reduce((sum, usage) => sum + usage.durationInMinutes, 0)),
    totalWorkshopUnitsUsed: computed(() => slots().reduce((sum, usage) => sum + usage.unitsUsed, 0)),
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
    updateUser: rxMethod<UpdateUserCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: UpdateUserCommand) => {
          return store._usersService.updateUser(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._userResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    updateUserContact: rxMethod<UpdateUserContactCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: UpdateUserContactCommand) => {
          return store._usersService.updateUserContact(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._userResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    updateUserAddress: rxMethod<UpdateUserAddressCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: UpdateUserAddressCommand) => {
          return store._usersService.updateUserAddress(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._userResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  // mutations of the RFID tag assignment – reload the assignment on success
  withMethods((store) => ({
    assignRfidTag: rxMethod<AssignRfidTagCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: AssignRfidTagCommand) => {
          return store._assignmentService.assignRfidTag(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._assignmentResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    unassignRfidTag: rxMethod<UnassignRfidTagCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: UnassignRfidTagCommand) => {
          return store._assignmentService.unassignRfidTag(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._assignmentResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  // mutations of the open session – reload the session on success
  withMethods((store) => ({
    closeSession: rxMethod<CloseWorkshopSessionCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: CloseWorkshopSessionCommand) => {
          return store._workshopSessionService.closeWorkshopSession(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._openSessionResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  // mutations of the certifications – reload the certifications on success
  withMethods((store) => ({
    pauseCertification: rxMethod<PauseToolCertificationCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: PauseToolCertificationCommand) => {
          return store._toolCertificationService.pause(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._certificationsResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    reactivateCertification: rxMethod<ReactivateToolCertificationCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: ReactivateToolCertificationCommand) => {
          return store._toolCertificationService.reactivate(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._certificationsResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    revokeCertification: rxMethod<RevokeToolCertificationCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: RevokeToolCertificationCommand) => {
          return store._toolCertificationService.revoke(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._certificationsResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
);
