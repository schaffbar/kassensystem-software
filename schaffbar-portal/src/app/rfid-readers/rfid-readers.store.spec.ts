import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { RfidReaderApiModel } from '../shared/api/rfid-readers/rfid-reader.api-model';
import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { toRfidReaderListItems } from './rfid-reader.mapper';
import { RfidReadersStore } from './rfid-readers.store';

describe('RfidReadersStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/rfid-readers`;
  const toolsUrl = `${environment.apiBaseUrl}/api/v1/tools`;
  const tools = [toolFixture({ id: 'tool-1', name: 'Bandsäge', rfidReaderId: 'id-1' })];
  const entities: RfidReaderApiModel[] = [
    {
      id: 'id-1',
      macAddress: 'AA:BB',
      type: RfidReaderType.SwitchBox,
      name: 'Reader 1',
      socketName: null,
      createdAt: '2026-01-01T10:00:00Z',
    },
    { id: 'id-2', macAddress: 'CC:DD', type: null, name: null, socketName: null, createdAt: '2026-01-02T10:00:00Z' },
  ];

  let store: InstanceType<typeof RfidReadersStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(RfidReadersStore);
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
  const expectToolsLoad = (): TestRequest => httpMock.expectOne((req) => req.method === 'GET' && req.url === toolsUrl);

  async function load(): Promise<void> {
    trigger();
    expectLoad().flush(entities);
    expectToolsLoad().flush(tools);
    await settle();
  }

  it('should load all entries with their tools on first use', async () => {
    await load();

    expect(store.rfidReaders()).toEqual(toRfidReaderListItems(entities, tools));
    expect(store.rfidReaders()[0].assignedTool).toEqual({ id: 'tool-1', name: 'Bandsäge' });
    expect(store.isLoading()).toBe(false);
  });

  it('should reload the entries on request', async () => {
    await load();

    store.reloadRfidReaders();
    trigger();
    expectLoad().flush([]);
    expectToolsLoad().flush([]);
    await settle();

    expect(store.rfidReaders()).toEqual([]);
  });

  it('should reload the entries after deleting one', async () => {
    await load();

    store.deleteRfidReader({ rfidReaderId: 'id-1' });
    trigger();
    expect(store.deleteRfidReader.isPending()).toBe(true);
    httpMock.expectOne({ method: 'DELETE', url: `${url}/id-1` }).flush(null);
    trigger();

    expectLoad().flush([entities[1]]);
    await settle();
    expect(store.rfidReaders()).toEqual(toRfidReaderListItems([entities[1]], tools));
    expect(store.deleteRfidReader.isPending()).toBe(false);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    trigger();
    expectLoad().flush([{ id: 'id-1' }]);
    expectToolsLoad().flush(tools);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.rfidReaders()).toEqual([]);
  });

  it('should show the readers without tools when the tools fail to load, but report the error', async () => {
    trigger();
    expectLoad().flush(entities);
    expectToolsLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.rfidReaders().map((rfidReader) => rfidReader.assignedTool)).toEqual([null, null]);
  });

  it('should expose a load error', async () => {
    trigger();
    expectLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    expectToolsLoad().flush(tools);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.rfidReaders()).toEqual([]);
  });
});
