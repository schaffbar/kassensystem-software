import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../environments/environment';
import { RfidReader } from './rfid-reader.model';
import { RfidReadersStore } from './rfid-readers.store';

describe('RfidReadersStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/rfid-readers`;
  const entities = [{ id: 'id-1' }, { id: 'id-2' }] as RfidReader[];

  let store: InstanceType<typeof RfidReadersStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(RfidReadersStore);
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
    expectLoad().flush(entities);
    await settle();
  }

  it('should load all entries on first use', async () => {
    await load();

    expect(store.entities()).toEqual(entities);
    expect(store.isLoading()).toBe(false);
  });

  it('should reload the entries on request', async () => {
    await load();

    store.reloadRfidReaders();
    trigger();
    expectLoad().flush([]);
    await settle();

    expect(store.entities()).toEqual([]);
  });

  it('should reload the entries after deleting one', async () => {
    await load();

    store.deleteRfidReader({ id: 'id-1' });
    trigger();
    expect(store.deleteRfidReader.isPending()).toBe(true);
    httpMock.expectOne({ method: 'DELETE', url: `${url}/id-1` }).flush(null);
    trigger();

    expectLoad().flush([entities[1]]);
    await settle();
    expect(store.entities()).toEqual([entities[1]]);
    expect(store.deleteRfidReader.isPending()).toBe(false);
  });

  it('should expose a load error', async () => {
    trigger();
    expectLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.entities()).toEqual([]);
  });
});
