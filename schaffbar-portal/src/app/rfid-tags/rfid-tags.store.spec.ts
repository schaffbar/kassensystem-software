import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { customerFixture } from '../shared/api/customers/customer.api-fixtures';
import { RfidTagAssignmentApiModel } from '../shared/api/rfid-tag-assignments/rfid-tag-assignment.api-model';
import { RfidTagApiModel } from '../shared/api/rfid-tags/rfid-tag.api-model';
import { RfidTagAssignmentStatus } from '../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';
import { toRfidTags, toWaitingUsers } from './rfid-tag.mapper';
import { RfidTagsStore } from './rfid-tags.store';

describe('RfidTagsStore', () => {
  const apiUrl = `${environment.apiBaseUrl}/api/v1`;
  const url = `${apiUrl}/rfid-tags`;
  const assignmentsUrl = `${apiUrl}/rfid-tag-assignments`;
  const usersUrl = `${apiUrl}/customers`;
  const entities: RfidTagApiModel[] = [
    { id: 'id-1', active: true },
    { id: 'id-2', active: false },
  ];
  const assignments: RfidTagAssignmentApiModel[] = [
    {
      id: 'assignment-1',
      customerId: 'user-1',
      assignmentType: RfidTagAssignmentType.Fixed,
      status: RfidTagAssignmentStatus.Assigned,
      rfidTagId: 'id-1',
      assignmentDate: '2025-02-03T10:00:00Z',
    },
    {
      id: 'assignment-2',
      customerId: 'user-2',
      assignmentType: RfidTagAssignmentType.Temporary,
      status: RfidTagAssignmentStatus.WaitingForAssignment,
      rfidTagId: null,
      assignmentDate: null,
    },
  ];
  const users = [customerFixture({ id: 'user-1' }), customerFixture({ id: 'user-2', firstName: 'Erika' })];

  let store: InstanceType<typeof RfidTagsStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(RfidTagsStore);
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

  const expectGet = (requestUrl: string): TestRequest =>
    httpMock.expectOne((req) => req.method === 'GET' && req.url === requestUrl);

  async function load(): Promise<void> {
    trigger();
    expectGet(url).flush(entities);
    expectGet(assignmentsUrl).flush(assignments);
    expectGet(usersUrl).flush(users);
    await settle();
  }

  it('should load the tags with their assignments and the waiting people on first use', async () => {
    await load();

    expect(store.rfidTags()).toEqual(toRfidTags(entities, assignments, users));
    expect(store.rfidTags().map((rfidTag) => rfidTag.state)).toEqual(['assigned', 'deactivated']);
    expect(store.waitingUsers()).toEqual(toWaitingUsers(assignments, users));
    expect(store.isLoading()).toBe(false);
  });

  it('should reload everything on request', async () => {
    await load();

    store.reloadRfidTags();
    trigger();
    expectGet(url).flush([]);
    expectGet(assignmentsUrl).flush([]);
    expectGet(usersUrl).flush(users);
    await settle();

    expect(store.rfidTags()).toEqual([]);
    expect(store.waitingUsers()).toEqual([]);
  });

  it('should send only the person when unassigning and reload the assignments', async () => {
    await load();

    store.unassignRfidTag({ rfidTagId: 'id-1', userId: 'user-1' });
    trigger();
    const unassign = httpMock.expectOne({ method: 'PUT', url: `${assignmentsUrl}/unassign` });
    expect(unassign.request.body).toEqual({ customerId: 'user-1' });
    unassign.flush(null);
    trigger();

    expectGet(assignmentsUrl).flush([assignments[1]]);
    await settle();
    expect(store.rfidTags()[0].state).toBe('free');
  });

  it('should reload the tags after deleting one', async () => {
    await load();

    store.deleteRfidTag({ rfidTagId: 'id-1' });
    trigger();
    expect(store.deleteRfidTag.isPending()).toBe(true);
    httpMock.expectOne({ method: 'DELETE', url: `${url}/id-1` }).flush(null);
    trigger();

    expectGet(url).flush([entities[1]]);
    await settle();
    expect(store.rfidTags()).toEqual(toRfidTags([entities[1]], assignments, users));
    expect(store.deleteRfidTag.isPending()).toBe(false);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    trigger();
    expectGet(url).flush(entities);
    expectGet(assignmentsUrl).flush([{ id: 'assignment-1' }]);
    expectGet(usersUrl).flush(users);
    await settle();

    expect(store.hasLoadError()).toBe(true);
  });

  it('should expose a load error', async () => {
    trigger();
    expectGet(url).flush('boom', { status: 500, statusText: 'Server Error' });
    expectGet(assignmentsUrl).flush(assignments);
    expectGet(usersUrl).flush(users);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.rfidTags()).toEqual([]);
  });
});
