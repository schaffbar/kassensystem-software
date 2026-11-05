import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../../../environments/environment';
import { RfidTagAssignmentApiModel } from '../../../shared/api/rfid-tag-assignments/rfid-tag-assignment.api-model';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { toRfidTagAssignment } from '../models/rfid-tag-assignment.mapper';
import { ToolCertificationApiModel } from '../../../shared/api/tool-certifications/tool-certification.api-model';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { toToolCertification } from '../models/tool-certification.mapper';
import { toolFixture } from '../../../shared/api/tools/tool.api-fixtures';
import { User } from '../models/user.model';
import { WorkshopSessionApiModel } from '../../../shared/api/workshop-sessions/workshop-session.api-model';
import {
  toolUsageSummaryFixture,
  workshopSessionFixture,
  workshopUsageFixture,
} from '../../../shared/api/workshop-sessions/workshop-session.api-fixtures';
import { toWorkshopSession } from '../models/workshop-session.mapper';
import { UserDetailStore } from './user-detail.store';

describe('UserDetailStore', () => {
  const api = `${environment.apiBaseUrl}/api/v1`;
  const user = { id: 'user-1', firstName: 'Max', lastName: 'Mustermann' } as User;
  const assignment: RfidTagAssignmentApiModel = {
    id: 'a-1',
    customerId: 'user-1',
    assignmentType: RfidTagAssignmentType.Fixed,
    status: RfidTagAssignmentStatus.Assigned,
    rfidTagId: 'tag-1',
    assignmentDate: '2026-10-31T10:00:00Z',
  };
  const session = workshopSessionFixture({
    workshopUsages: [
      workshopUsageFixture({ id: 'u-1', exitTime: '2026-10-31T10:30:00Z', durationInMinutes: 30, unitsUsed: 1 }),
      workshopUsageFixture({ id: 'u-2', exitTime: '2026-10-31T11:45:00Z', durationInMinutes: 45, unitsUsed: 2 }),
    ],
    // the totals come from the backend (incl. running usages) and are not summed up again
    totalWorkshopDurationInMinutes: 80,
    totalWorkshopUnits: 4,
    toolUsageSummaries: [],
  });
  const certifications: ToolCertificationApiModel[] = [
    {
      id: 'cert-1',
      customerId: 'user-1',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Active,
      certifiedAt: '2026-01-01T10:00:00Z',
      certifiedBy: null,
    },
  ];

  let store: InstanceType<typeof UserDetailStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), UserDetailStore],
    });
    store = TestBed.inject(UserDetailStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

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
  const expectTools = () => expectGet(`${api}/tools`);

  async function loadUser(
    options: { assignments?: RfidTagAssignmentApiModel[]; session?: WorkshopSessionApiModel | null } = {},
  ) {
    store.setUserId('user-1');
    trigger();
    expectUser().flush(user);
    expectAssignment().flush(options.assignments ?? [assignment]);
    expectSession().flush(options.session === undefined ? session : options.session);
    expectCertifications().flush(certifications);
    expectTools().flush([toolFixture()]);
    await settle();
  }

  it('should load only the tools without an id', async () => {
    trigger();

    expectTools().flush([toolFixture()]);
    httpMock.expectNone(() => true);
    await settle();
    expect(store.user()).toBeNull();
    expect(store.tools()).toEqual([{ id: 'tool-1', name: 'Bandsäge' }]);
  });

  it('should load user, assignment, open session and certifications when the id is set', async () => {
    await loadUser();

    expect(store.user()).toEqual(user);
    expect(store.userName()).toBe('Max Mustermann');
    expect(store.rfidTagAssignment()).toEqual(toRfidTagAssignment(assignment));
    expect(store.openSession()).toEqual(toWorkshopSession(session));
    expect(store.totalWorkshopDurationInMinutes()).toBe(80);
    expect(store.totalWorkshopUnits()).toBe(4);
    expect(store.hasRunningUsage()).toBe(false);
    expect(store.toolCertifications()).toEqual(certifications.map(toToolCertification));
    expect(store.tools()).toEqual([{ id: 'tool-1', name: 'Bandsäge' }]);
    expect(store.hasLoadError()).toBe(false);
  });

  it('should treat an empty assignment list and an empty session body as "none", not as error', async () => {
    await loadUser({ assignments: [], session: null });

    expect(store.rfidTagAssignment()).toBeNull();
    expect(store.openSession()).toBeNull();
    expect(store.slots()).toEqual([]);
    expect(store.hasRunningUsage()).toBe(false);
    expect(store.hasLoadError()).toBe(false);
  });

  it('should detect a running workshop usage', async () => {
    const running = workshopSessionFixture({
      workshopUsages: [workshopUsageFixture({ exitTime: null, durationInMinutes: null, unitsUsed: null })],
      toolUsageSummaries: [],
    });

    await loadUser({ session: running });

    expect(store.hasRunningUsage()).toBe(true);
  });

  it('should detect an active tool usage', async () => {
    const running = workshopSessionFixture({ toolUsageSummaries: [toolUsageSummaryFixture({ active: true })] });

    await loadUser({ session: running });

    expect(store.hasRunningUsage()).toBe(true);
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
    expect(store.rfidTagAssignment()).toEqual(toRfidTagAssignment(assignment));
  });

  it('should only reload the assignment after unassigning the RFID tag', async () => {
    await loadUser();

    store.unassignRfidTag({ customerId: 'user-1' });
    trigger();
    const unassign = httpMock.expectOne(
      (req) => req.method === 'PUT' && req.url === `${api}/rfid-tag-assignments/unassign`,
    );
    expect(unassign.request.body).toEqual({ customerId: 'user-1' });
    unassign.flush(null);
    trigger();

    expectAssignment().flush([]);
    await settle();
    expect(store.rfidTagAssignment()).toBeNull();
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

  it('should expose a load error for a session that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    await loadUser({ session: { ...session, startTime: 'heute' } });

    expect(store.hasLoadError()).toBe(true);
    expect(store.openSession()).toBeNull();
  });

  it('should expose a load error', async () => {
    store.setUserId('user-1');
    trigger();
    expectUser().flush('not found', { status: 404, statusText: 'Not Found' });
    expectAssignment().flush([]);
    expectSession().flush(null);
    expectCertifications().flush([]);
    expectTools().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.user()).toBeNull();
  });
});
