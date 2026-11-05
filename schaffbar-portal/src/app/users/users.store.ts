import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { CustomersApiService } from '../shared/api/customers/customers.api-service';
import { mutation } from '../shared/state/mutation';
import { toCreateCustomerRequest, toUser } from './user.mapper';
import { CreateUserCommand, DeleteUserCommand } from './user.model';

export const UsersStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const customersApi = inject(CustomersApiService);
    return {
      _customersApi: customersApi,
      // loads on first use of the store – replaces withEntities + rxMethod + onInit
      _usersResource: customersApi.customersResource(),
    };
  }),
  withComputed(({ _usersResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_usersResource.hasValue() ? _usersResource.value().map(toUser) : [])),
    isLoading: computed(() => _usersResource.isLoading()),
    hasLoadError: computed(() => !!_usersResource.error()),
  })),
  withMethods((store) => ({
    reloadUsers(): void {
      store._usersResource.reload();
    },
    createUser: mutation<CreateUserCommand>({
      request: (command) => store._customersApi.createCustomer(toCreateCustomerRequest(command)),
      onSuccess: () => store._usersResource.reload(),
    }),
    deleteUser: mutation<DeleteUserCommand>({
      request: (command) => store._customersApi.deleteCustomer(command.id),
      onSuccess: () => store._usersResource.reload(),
    }),
  })),
);
