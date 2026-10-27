import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { mutation } from '../../../shared/state/mutation';
import { CreateUserCommand, DeleteUserCommand } from '../models/user.model';
import { UsersService } from '../services/users.service';

export const UsersStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const usersService = inject(UsersService);
    return {
      _usersService: usersService,
      // loads on first use of the store – replaces withEntities + rxMethod + onInit
      _usersResource: usersService.usersResource(),
    };
  }),
  withComputed(({ _usersResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_usersResource.hasValue() ? _usersResource.value() : [])),
    isLoading: computed(() => _usersResource.isLoading()),
    hasLoadError: computed(() => !!_usersResource.error()),
  })),
  withMethods((store) => ({
    reloadUsers(): void {
      store._usersResource.reload();
    },
    createUser: mutation<CreateUserCommand>({
      request: (command) => store._usersService.createUser(command),
      onSuccess: () => store._usersResource.reload(),
    }),
    deleteUser: mutation<DeleteUserCommand>({
      request: (command) => store._usersService.deleteUser(command),
      onSuccess: () => store._usersResource.reload(),
    }),
  })),
);
