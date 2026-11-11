import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { customerFixture } from '../shared/api/customers/customer.api-fixtures';
import { RfidReaderApiModel } from '../shared/api/rfid-readers/rfid-reader.api-model';
import { ToolDashboardEntryApiModel } from '../shared/api/tool-dashboard/tool-dashboard.api-model';
import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { WorkshopDashboardEntryApiModel } from '../shared/api/workshop-dashboard/workshop-dashboard.api-model';
import { AgeGroup } from '../shared/models/age-group';
import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { WlanRelaisType } from '../shared/models/wlan-relais-type';
import { toActiveTool, toActiveUser, toRfidReader, toTool } from './dashboard.mapper';
import { DashboardStore } from './dashboard.store';

describe('DashboardStore', () => {
  const api = `${environment.apiBaseUrl}/api/v1`;
  // the band saw is set up, the lathe has no hardware yet
  const tools = [
    toolFixture({ rfidReaderId: 'reader-1', wlanRelaisType: WlanRelaisType.Shelly1 }),
    toolFixture({ id: 'tool-2', name: 'Drechselbank' }),
  ];
  const activeUsers: WorkshopDashboardEntryApiModel[] = [
    { customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann', entryTime: '2026-10-27T08:30:00Z' },
    { customerId: 'user-2', firstName: 'Lena', lastName: 'Klein', entryTime: '2026-10-27T09:00:00Z' },
  ];
  const activeTools: ToolDashboardEntryApiModel[] = [
    { toolId: 'tool-1', toolName: 'Bandsäge', customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann' },
  ];
  const users = [
    customerFixture(),
    customerFixture({ id: 'user-2', ageGroup: AgeGroup.Under16 }),
    customerFixture({ id: 'user-3', ageGroup: AgeGroup.Under18 }),
  ];
  const rfidReaders: RfidReaderApiModel[] = [
    {
      id: 'reader-1',
      macAddress: 'AA:BB',
      type: RfidReaderType.SwitchBox,
      name: 'Schaltbox Bandsäge',
      socketName: null,
      createdAt: '2026-01-01T10:00:00Z',
    },
    {
      id: 'reader-2',
      macAddress: 'CC:DD',
      type: null,
      name: null,
      socketName: null,
      createdAt: '2026-01-01T10:00:00Z',
    },
  ];

  let store: InstanceType<typeof DashboardStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), DashboardStore],
    });
    store = TestBed.inject(DashboardStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  // lets the resources react and send their requests
  function trigger(): void {
    TestBed.tick();
  }

  // processes the flushed responses
  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  const expectGet = (path: string): TestRequest => httpMock.expectOne({ method: 'GET', url: `${api}${path}` });

  function respond(overrides: { activeUsers?: unknown } = {}): void {
    expectGet('/workshop-dashboard/active-users').flush(overrides.activeUsers ?? activeUsers);
    expectGet('/tool-dashboard/active-tools').flush(activeTools);
    expectGet('/tools').flush(tools);
    expectGet('/customers').flush(users);
    expectGet('/rfid-readers').flush(rfidReaders);
  }

  async function load(): Promise<void> {
    trigger();
    respond();
    await settle();
  }

  it('should load everything the overview shows when it opens', async () => {
    await load();

    expect(store.activeUsers()).toEqual(activeUsers.map(toActiveUser));
    expect(store.activeTools()).toEqual(activeTools.map(toActiveTool));
    expect(store.tools()).toEqual(tools.map(toTool));
    expect(store.rfidReaders()).toEqual(rfidReaders.map(toRfidReader));
    expect(store.users().map((user) => user.id)).toEqual(['user-1', 'user-2', 'user-3']);
    expect(store.isLoading()).toBe(false);
    expect(store.updatedAt()).toBeInstanceOf(Date);
  });

  it('should count the minors in the workshop and find what needs attention', async () => {
    await load();

    // user-3 is under 18 too, but not in the workshop
    expect(store.minorsInWorkshopCount()).toBe(1);
    expect(store.toolsWithoutHardware().map((tool) => tool.id)).toEqual(['tool-2']);
    expect(store.rfidReadersWithoutType().map((rfidReader) => rfidReader.id)).toEqual(['reader-2']);
  });

  it('should be loading until everything has arrived', async () => {
    trigger();
    expect(store.isLoading()).toBe(true);
    expect(store.updatedAt()).toBeNull();

    respond();
    await settle();
    expect(store.isLoading()).toBe(false);
  });

  it('should reload everything without showing a load', async () => {
    await load();

    store.reloadDashboard();
    trigger();
    expect(store.isLoading()).toBe(false);
    respond({ activeUsers: [] });
    await settle();

    expect(store.activeUsers()).toEqual([]);
  });

  it('should reload everything every interval', async () => {
    await load();
    // only the interval – whenStable needs real timeouts
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    TestBed.runInInjectionContext(() => store.pollDashboard(60_000));
    trigger();

    vi.advanceTimersToNextTimer();
    trigger();
    respond({ activeUsers: [] });
    await settle();

    expect(store.activeUsers()).toEqual([]);
  });

  it('should expose a load error of any list', async () => {
    trigger();
    expectGet('/workshop-dashboard/active-users').flush(activeUsers);
    expectGet('/tool-dashboard/active-tools').flush('boom', { status: 500, statusText: 'Server Error' });
    expectGet('/tools').flush(tools);
    expectGet('/customers').flush(users);
    expectGet('/rfid-readers').flush(rfidReaders);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.activeUsers()).toEqual(activeUsers.map(toActiveUser));
    expect(store.activeTools()).toEqual([]);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    trigger();
    respond({
      activeUsers: [{ customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann', entryTime: 'gestern' }],
    });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.activeUsers()).toEqual([]);
  });
});
