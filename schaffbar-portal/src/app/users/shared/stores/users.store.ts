import { inject } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withProps } from '@ngrx/signals';
import { setAllEntities, setEntity, withEntities } from '@ngrx/signals/entities';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, pipe, tap } from 'rxjs';

import { mutation } from '../../../shared/state/mutation';
import { setError, setFulfilled, setPending, withRequestStatus } from '../../../shared/state/request-status.feature';
import { CreateUserCommand, DeleteUserCommand, User } from '../models/user.model';
import { UsersService } from '../services/users.service';

export const UsersStore = signalStore(
  { providedIn: 'root' },
  withEntities<User>(),
  withRequestStatus(),
  withProps(() => ({
    _usersService: inject(UsersService),
  })),
  withMethods((store) => ({
    setUser(user: User): void {
      patchState(store, setEntity(user));
    },
    loadAllUsers: rxMethod<void>(
      pipe(
        tap(() => patchState(store, setPending())),
        // delay(100), // TODO: Simulate network latency
        exhaustMap(() => {
          return store._usersService.getUsers().pipe(
            // delay(400),
            tapResponse({
              next: (users) => patchState(store, setAllEntities(users), setFulfilled()),
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  withMethods((store) => ({
    createUser: mutation<CreateUserCommand>({
      request: (command) => store._usersService.createUser(command),
      onSuccess: () => store.loadAllUsers(),
    }),
    deleteUser: mutation<DeleteUserCommand>({
      request: (command) => store._usersService.deleteUser(command),
      onSuccess: () => store.loadAllUsers(),
    }),
  })),
  withHooks((store) => ({
    onInit() {
      store.loadAllUsers();
    },
  })),
);
