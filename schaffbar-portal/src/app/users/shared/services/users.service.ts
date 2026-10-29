import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { format } from 'date-fns';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateUserCommand,
  DeleteUserCommand,
  UpdateUserAddressCommand,
  UpdateUserCommand,
  UpdateUserContactCommand,
  User,
} from '../models/user.model';

const USERS_API_URL = `${environment.apiBaseUrl}/api/v1/customers`;

@Service()
export class UsersService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads all users; reload with `reload()`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  usersResource(): HttpResourceRef<User[] | undefined> {
    return httpResource<User[]>(() => USERS_API_URL);
  }

  /**
   * Loads the user whenever the id changes; no request while the id is `null`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  userResource(id: () => string | null): HttpResourceRef<User | undefined> {
    return httpResource<User>(() => {
      const userId = id();
      return userId ? `${USERS_API_URL}/${userId}` : undefined;
    });
  }

  // --------------------------------------------------------------------------
  // commands

  createUser(command: CreateUserCommand): Observable<void> {
    const requestBody = {
      ...command,
      dateOfBirth: format(command.dateOfBirth, 'yyyy-MM-dd'),
    };

    return this.http.post<void>(USERS_API_URL, requestBody);
  }

  updateUser(command: UpdateUserCommand): Observable<void> {
    const requestBody = {
      ...command,
      dateOfBirth: format(command.dateOfBirth, 'yyyy-MM-dd'),
    };

    return this.http.put<void>(`${USERS_API_URL}/${command.id}`, requestBody);
  }

  updateUserContact(command: UpdateUserContactCommand): Observable<void> {
    return this.http.put<void>(`${USERS_API_URL}/${command.id}/contact`, command);
  }

  updateUserAddress(command: UpdateUserAddressCommand): Observable<void> {
    return this.http.put<void>(`${USERS_API_URL}/${command.id}/address`, command);
  }

  deleteUser(command: DeleteUserCommand): Observable<void> {
    return this.http.delete<void>(`${USERS_API_URL}/${command.id}`);
  }
}
