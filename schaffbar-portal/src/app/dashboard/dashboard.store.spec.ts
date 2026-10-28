import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../environments/environment';
import { ActiveTool, ActiveUser } from './dashboard.model';
import { DashboardStore } from './dashboard.store';

describe('DashboardStore', () => {
  const activeUsersUrl = `${environment.apiBaseUrl}/api/v1/workshop-dashboard/active-users`;
  const activeToolsUrl = `${environment.apiBaseUrl}/api/v1/tool-dashboard/active-tools`;
  const activeUsers = [{ customerId: 'user-1', firstName: 'Max' }] as ActiveUser[];
  const activeTools = [{ toolId: 'tool-1' }] as unknown as ActiveTool[];

  let store: InstanceType<typeof DashboardStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), DashboardStore],
    });
    store = TestBed.inject(DashboardStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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

    expect(store.activeUsers()).toEqual(activeUsers);
    expect(store.activeTools()).toEqual(activeTools);
    expect(store.isLoading()).toBe(false);
  });

  it('should be loading until both lists have arrived', async () => {
    trigger();
    expectUsers().flush(activeUsers);
    // not settle(): whenStable() would wait for the open tools request
    await new Promise((resolve) => setTimeout(resolve));
    expect(store.activeUsers()).toEqual(activeUsers);
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
    expect(store.activeTools()).toEqual(activeTools);
  });

  it('should reload only the active tools', async () => {
    await load();

    store.reloadActiveTools();
    trigger();
    expectTools().flush([]);
    await settle();

    expect(store.activeTools()).toEqual([]);
    expect(store.activeUsers()).toEqual(activeUsers);
  });

  it('should expose a load error of either list', async () => {
    trigger();
    expectUsers().flush(activeUsers);
    expectTools().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.activeUsers()).toEqual(activeUsers);
    expect(store.activeTools()).toEqual([]);
  });
});
