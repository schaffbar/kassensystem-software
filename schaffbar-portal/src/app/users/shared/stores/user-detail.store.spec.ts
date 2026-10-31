import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../../environments/environment';
import { RfidTagAssignment, RfidTagAssignmentStatus, RfidTagAssignmentType } from '../models/rfid-tag-assignment.model';
import { ToolCertification } from '../models/tool-certification.model';
import { User } from '../models/user.model';
import { WorkshopSession } from '../models/workshop-session.model';
import { UserDetailStore } from './user-detail.store';

describe('UserDetailStore', () => {
  const api = `${environment.apiBaseUrl}/api/v1`;
  const user = { id: 'user-1', firstName: 'Max', lastName: 'Mustermann' } as User;
  const assignment = { id: 'a-1', customerId: 'user-1', status: RfidTagAssignmentStatus.Assigned } as RfidTagAssignment;
  const session = {
    id: 's-1',
    workshopUsages: [
      { durationInMinutes: 30, unitsUsed: 1 },
      { durationInMinutes: 45, unitsUsed: 2 },
    ],
    // the totals come from the backend (incl. running usages) and are not summed up again
    totalWorkshopDurationInMinutes: 80,
    totalWorkshopUnits: 4,
    toolUsageSummaries: [],
  } as unknown as WorkshopSession;
  const certifications = [{ id: 'cert-1', customerId: 'user-1', toolId: 'tool-1' }] as ToolCertification[];

  let store: InstanceType<typeof UserDetailStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), UserDetailStore],
    });
    store = TestBed.inject(UserDetailStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // lets the resources react to signal changes and send their requests
  function trigger(): void {
    TestBed.tick();
  }

  // processes the flushed responses
  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  const expectGet = (url: string, param?: [string, string]): TestRequest =>
    httpMock.expectOne(
      (req) => req.method === 'GET' && req.url === url && (!param || req.params.get(param[0]) === param[1]),
    );
  const expectUser = () => expectGet(`${api}/customers/user-1`);
  const expectAssignment = () => expectGet(`${api}/rfid-tag-assignments`, ['customer-id', 'user-1']);
  const expectSession = () => expectGet(`${api}/workshop-sessions/user-1/active`);
  const expectCertifications = () => expectGet(`${api}/tool-certifications`, ['customerId', 'user-1']);

  async function loadUser(options: { assignments?: RfidTagAssignment[]; session?: WorkshopSession | null } = {}) {
    store.setUserId('user-1');
    trigger();
    expectUser().flush(user);
    expectAssignment().flush(options.assignments ?? [assignment]);
    expectSession().flush(options.session === undefined ? session : options.session);
    expectCertifications().flush(certifications);
    await settle();
  }

  it('should not load anything without an id', () => {
    trigger();

    httpMock.expectNone(() => true);
    expect(store.user()).toBeNull();
  });

  it('should load user, assignment, open session and certifications when the id is set', async () => {
    await loadUser();

    expect(store.user()).toEqual(user);
    expect(store.userName()).toBe('Max Mustermann');
    expect(store.rfidTagAssignment()).toEqual(assignment);
    expect(store.openSession()).toEqual(session);
    expect(store.totalWorkshopDurationInMinutes()).toBe(80);
    expect(store.totalWorkshopUnits()).toBe(4);
    expect(store.toolCertifications()).toEqual(certifications);
    expect(store.hasLoadError()).toBe(false);
  });

  it('should treat an empty assignment list and an empty session body as "none", not as error', async () => {
    await loadUser({ assignments: [], session: null });

    expect(store.rfidTagAssignment()).toBeNull();
    expect(store.openSession()).toBeNull();
    expect(store.slots()).toEqual([]);
    expect(store.hasLoadError()).toBe(false);
  });

  it('should only reload the assignment after assigning an RFID tag', async () => {
    await loadUser({ assignments: [] });

    store.assignRfidTag({ customerId: 'user-1', assignmentType: RfidTagAssignmentType.Fixed });
    trigger();
    const assign = httpMock.expectOne((req) => req.method === 'POST' && req.url === `${api}/rfid-tag-assignments`);
    expect(assign.request.body).toEqual({ customerId: 'user-1', assignmentType: 'FIXED' });
    assign.flush(null);
    trigger();

    expectAssignment().flush([assignment]);
    await settle();
    expect(store.rfidTagAssignment()).toEqual(assignment);
  });

  it('should only reload the open session after closing it', async () => {
    await loadUser();

    store.closeSession({ customerId: 'user-1' });
    trigger();
    httpMock
      .expectOne((req) => req.method === 'PUT' && req.url === `${api}/workshop-sessions/user-1/close`)
      .flush(null);
    trigger();

    expectSession().flush(null);
    await settle();
    expect(store.openSession()).toBeNull();
  });

  it('should only reload the certifications after pausing one', async () => {
    await loadUser();

    store.pauseCertification({ customerId: 'user-1', toolId: 'tool-1' });
    trigger();
    httpMock
      .expectOne(
        (req) => req.method === 'PUT' && req.url === `${api}/tool-certifications/customers/user-1/tools/tool-1/pause`,
      )
      .flush(null);
    trigger();

    expectCertifications().flush([]);
    await settle();
    expect(store.toolCertifications()).toEqual([]);
  });

  it('should expose a load error', async () => {
    store.setUserId('user-1');
    trigger();
    expectUser().flush('not found', { status: 404, statusText: 'Not Found' });
    expectAssignment().flush([]);
    expectSession().flush(null);
    expectCertifications().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.user()).toBeNull();
  });
});
