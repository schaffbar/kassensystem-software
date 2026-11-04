import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { ToolDashboardEntryApiModel } from '../shared/api/tool-dashboard/tool-dashboard.api-model';
import { WorkshopDashboardEntryApiModel } from '../shared/api/workshop-dashboard/workshop-dashboard.api-model';
import { toActiveTool, toActiveUser } from './dashboard.mapper';
import { DashboardStore } from './dashboard.store';

describe('DashboardStore', () => {
  const activeUsersUrl = `${environment.apiBaseUrl}/api/v1/workshop-dashboard/active-users`;
  const activeToolsUrl = `${environment.apiBaseUrl}/api/v1/tool-dashboard/active-tools`;
  const activeUsers: WorkshopDashboardEntryApiModel[] = [
    { customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann', entryTime: '2026-10-27T08:30:00Z' },
  ];
  const activeTools: ToolDashboardEntryApiModel[] = [
    { toolId: 'tool-1', toolName: 'Bandsäge', customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann' },
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
  });

  // lets the resources react and send their requests
  function trigger(): void {
    TestBed.tick();
  }

  // processes the flushed responses
  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  const expectUsers = (): TestRequest => httpMock.expectOne({ method: 'GET', url: activeUsersUrl });
  const expectTools = (): TestRequest => httpMock.expectOne({ method: 'GET', url: activeToolsUrl });

  async function load(): Promise<void> {
    trigger();
    expectUsers().flush(activeUsers);
    expectTools().flush(activeTools);
    await settle();
  }

  it('should load the active users and tools when the dashboard opens', async () => {
    await load();

    expect(store.activeUsers()).toEqual(activeUsers.map(toActiveUser));
    expect(store.activeTools()).toEqual(activeTools.map(toActiveTool));
    expect(store.isLoading()).toBe(false);
  });

  it('should be loading until both lists have arrived', async () => {
    trigger();
    expectUsers().flush(activeUsers);
    // not settle(): whenStable() would wait for the open tools request
    await new Promise((resolve) => setTimeout(resolve));
    expect(store.activeUsers()).toEqual(activeUsers.map(toActiveUser));
    expect(store.isLoading()).toBe(true);

    expectTools().flush(activeTools);
    await settle();
    expect(store.isLoading()).toBe(false);
  });

  it('should reload only the active users', async () => {
    await load();

    store.reloadActiveUsers();
    trigger();
    expectUsers().flush([]);
    await settle();

    expect(store.activeUsers()).toEqual([]);
    expect(store.activeTools()).toEqual(activeTools.map(toActiveTool));
  });

  it('should reload only the active tools', async () => {
    await load();

    store.reloadActiveTools();
    trigger();
    expectTools().flush([]);
    await settle();

    expect(store.activeTools()).toEqual([]);
    expect(store.activeUsers()).toEqual(activeUsers.map(toActiveUser));
  });

  it('should expose a load error of either list', async () => {
    trigger();
    expectUsers().flush(activeUsers);
    expectTools().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.activeUsers()).toEqual(activeUsers.map(toActiveUser));
    expect(store.activeTools()).toEqual([]);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    trigger();
    expectUsers().flush([{ customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann', entryTime: 'gestern' }]);
    expectTools().flush(activeTools);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.activeUsers()).toEqual([]);
  });
});
