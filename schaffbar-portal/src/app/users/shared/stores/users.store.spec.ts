import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../../environments/environment';
import { CreateUserCommand, User } from '../models/user.model';
import { UsersStore } from './users.store';

describe('UsersStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/customers`;
  const users = [{ id: 'user-1', firstName: 'Max', lastName: 'Mustermann' }] as User[];

  let store: InstanceType<typeof UsersStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(UsersStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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

    expect(store.entities()).toEqual(users);
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
    expect(create.request.body).toEqual({ ...command, dateOfBirth: '1990-01-31' });
    create.flush({});
    trigger();

    expectLoad().flush([...users, { id: 'user-2' } as User]);
    await settle();
    expect(store.entities()).toHaveLength(2);
  });

  it('should reload the users after deleting one', async () => {
    await load();

    store.deleteUser({ id: 'user-1' });
    trigger();
    httpMock.expectOne({ method: 'DELETE', url: `${url}/user-1` }).flush(null);
    trigger();

    expectLoad().flush([]);
    await settle();
    expect(store.entities()).toEqual([]);
  });

  it('should expose a load error', async () => {
    trigger();
    expectLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.entities()).toEqual([]);
  });
});
