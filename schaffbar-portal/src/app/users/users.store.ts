import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';
import { map, Observable } from 'rxjs';

import { CustomersApiService } from '../shared/api/customers/customers.api-service';
import { RfidTagAssignmentsApiService } from '../shared/api/rfid-tag-assignments/rfid-tag-assignments.api-service';
import { WorkshopDashboardApiService } from '../shared/api/workshop-dashboard/workshop-dashboard.api-service';
import { WorkshopSessionsApiService } from '../shared/api/workshop-sessions/workshop-sessions.api-service';
import { mutation } from '../shared/state/mutation';
import { toCreateCustomerRequest, toUserListItems } from './user.mapper';
import { CreateUserCommand, DeleteUserCommand } from './user.model';

export const UsersStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const customersApi = inject(CustomersApiService);
    return {
      _customersApi: customersApi,
      _workshopSessionsApi: inject(WorkshopSessionsApiService),
      // all load on first use of the store; assignments and active users tell who has a tag and who is in the workshop
      _usersResource: customersApi.customersResource(),
      _assignmentsResource: inject(RfidTagAssignmentsApiService).rfidTagAssignmentsResource(),
      _activeUsersResource: inject(WorkshopDashboardApiService).activeUsersResource(),
    };
  }),
  withComputed(({ _usersResource, _assignmentsResource, _activeUsersResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    users: computed(() =>
      _usersResource.hasValue()
        ? toUserListItems(
            _usersResource.value(),
            _assignmentsResource.hasValue() ? _assignmentsResource.value() : [],
            _activeUsersResource.hasValue() ? _activeUsersResource.value() : [],
          )
        : [],
    ),
    isLoading: computed(
      () => _usersResource.isLoading() || _assignmentsResource.isLoading() || _activeUsersResource.isLoading(),
    ),
    hasLoadError: computed(
      () => !!_usersResource.error() || !!_assignmentsResource.error() || !!_activeUsersResource.error(),
    ),
  })),
  withMethods((store) => ({
    reloadUsers(): void {
      store._usersResource.reload();
      store._assignmentsResource.reload();
      store._activeUsersResource.reload();
    },
    // a user with an open session is not deleted – the session still has to be closed and paid
    canDeleteUser(userId: string): Observable<boolean> {
      return store._workshopSessionsApi.getActiveWorkshopSession(userId).pipe(map((session) => !session));
    },
    createUser: mutation<CreateUserCommand>({
      request: (command) => store._customersApi.createCustomer(toCreateCustomerRequest(command)),
      onSuccess: () => store._usersResource.reload(),
    }),
    deleteUser: mutation<DeleteUserCommand>({
      request: (command) => store._customersApi.deleteCustomer(command.userId),
      onSuccess: () => store._usersResource.reload(),
    }),
  })),
);
