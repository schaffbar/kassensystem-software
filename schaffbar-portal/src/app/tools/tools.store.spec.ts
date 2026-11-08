import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { ToolDashboardEntryApiModel } from '../shared/api/tool-dashboard/tool-dashboard.api-model';
import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { CertificationRequirement } from '../shared/models/certification-requirement';
import { ToolArea } from '../shared/models/tool-area';
import { toToolListItems } from './tool.mapper';
import { ToolsStore } from './tools.store';

describe('ToolsStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/tools`;
  const activeToolsUrl = `${environment.apiBaseUrl}/api/v1/tool-dashboard/active-tools`;
  const tools = [toolFixture()];
  const activeTools: ToolDashboardEntryApiModel[] = [
    { toolId: 'tool-1', toolName: 'Bandsäge', customerId: 'user-1', firstName: 'Anna', lastName: 'Becker' },
  ];

  let store: InstanceType<typeof ToolsStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(ToolsStore);
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
  const expectActiveToolsLoad = (): TestRequest =>
    httpMock.expectOne((req) => req.method === 'GET' && req.url === activeToolsUrl);

  async function load(): Promise<void> {
    trigger();
    expectLoad().flush(tools);
    expectActiveToolsLoad().flush(activeTools);
    await settle();
  }

  it('should load all tools with the person using them on first use', async () => {
    await load();

    expect(store.tools()).toEqual(toToolListItems(tools, activeTools));
    expect(store.tools()[0].usedBy).toBe('Anna Becker');
    expect(store.isLoading()).toBe(false);
  });

  it('should reload the tools on request', async () => {
    await load();

    store.reloadTools();
    trigger();
    expectLoad().flush([]);
    expectActiveToolsLoad().flush([]);
    await settle();

    expect(store.tools()).toEqual([]);
  });

  it('should reload the tools after creating one', async () => {
    await load();

    store.createTool({
      name: 'Drechselbank',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    });
    trigger();
    const create = httpMock.expectOne((req) => req.method === 'POST' && req.url === url);
    expect(create.request.body).toEqual({
      name: 'Drechselbank',
      description: null,
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    });
    expect(store.createTool.isPending()).toBe(true);
    create.flush({});
    trigger();

    expectLoad().flush([...tools, toolFixture({ id: 'tool-2', name: 'Drechselbank' })]);
    await settle();
    expect(store.tools()).toHaveLength(2);
    expect(store.createTool.isPending()).toBe(false);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    trigger();
    expectLoad().flush([{ id: 'id-1' }]);
    expectActiveToolsLoad().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.tools()).toEqual([]);
  });

  it('should expose a load error', async () => {
    trigger();
    expectLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    expectActiveToolsLoad().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.tools()).toEqual([]);
  });
});
