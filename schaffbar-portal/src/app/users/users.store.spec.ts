import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { customerFixture } from '../shared/api/customers/customer.api-fixtures';
import { toUser } from './user.mapper';
import { CreateUserCommand } from './user.model';
import { UsersStore } from './users.store';

describe('UsersStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/customers`;
  const users = [customerFixture()];

  let store: InstanceType<typeof UsersStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(UsersStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  // lets the resource react and send its request
  function trigger(): void {
    TestBed.tick();
  }

  // processes the flushed response
  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  const expectLoad = (): TestRequest => httpMock.expectOne((req) => req.method === 'GET' && req.url === url);

  async function load(): Promise<void> {
    trigger();
    expectLoad().flush(users);
    await settle();
  }

  it('should load all users on first use', async () => {
    await load();

    expect(store.users()).toEqual(users.map(toUser));
    expect(store.isLoading()).toBe(false);
  });

  it('should send the date of birth as ISO date and reload the users after creating one', async () => {
    await load();

    const command: CreateUserCommand = {
      firstName: 'Erika',
      lastName: 'Musterfrau',
      dateOfBirth: new Date(1990, 0, 31),
      clubMember: false,
      email: 'erika@example.org',
      addressLine1: 'Hauptstraße 1',
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    };
    store.createUser(command);
    trigger();
    const create = httpMock.expectOne((req) => req.method === 'POST' && req.url === url);
    expect(create.request.body).toEqual({ ...command, dateOfBirth: '1990-01-31', phone: null, addressLine2: null });
    create.flush({});
    trigger();

    expectLoad().flush([...users, customerFixture({ id: 'user-2' })]);
    await settle();
    expect(store.users()).toHaveLength(2);
  });

  it('should reload the users after deleting one', async () => {
    await load();

    store.deleteUser({ userId: 'user-1' });
    trigger();
    httpMock.expectOne({ method: 'DELETE', url: `${url}/user-1` }).flush(null);
    trigger();

    expectLoad().flush([]);
    await settle();
    expect(store.users()).toEqual([]);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    trigger();
    expectLoad().flush([{ id: 'id-1' }]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.users()).toEqual([]);
  });

  it('should expose a load error', async () => {
    trigger();
    expectLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.users()).toEqual([]);
  });
});
