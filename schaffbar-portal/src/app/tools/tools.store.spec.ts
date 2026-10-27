import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../environments/environment';
import { CertificationRequirement, Tool, ToolArea } from './tool.model';
import { ToolsStore } from './tools.store';

describe('ToolsStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/tools`;
  const tools = [{ id: 'tool-1', name: 'Bandsäge' }] as Tool[];

  let store: InstanceType<typeof ToolsStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(ToolsStore);
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
    expectLoad().flush(tools);
    await settle();
  }

  it('should load all tools on first use', async () => {
    await load();

    expect(store.entities()).toEqual(tools);
    expect(store.isLoading()).toBe(false);
  });

  it('should reload the tools on request', async () => {
    await load();

    store.reloadTools();
    trigger();
    expectLoad().flush([]);
    await settle();

    expect(store.entities()).toEqual([]);
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
    expect(store.createTool.isPending()).toBe(true);
    create.flush({});
    trigger();

    expectLoad().flush([...tools, { id: 'tool-2', name: 'Drechselbank' } as Tool]);
    await settle();
    expect(store.entities()).toHaveLength(2);
    expect(store.createTool.isPending()).toBe(false);
  });

  it('should expose a load error', async () => {
    trigger();
    expectLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.entities()).toEqual([]);
  });
});
