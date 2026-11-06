import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { RfidTagApiModel } from '../shared/api/rfid-tags/rfid-tag.api-model';
import { toRfidTag } from './rfid-tag.mapper';
import { RfidTagsStore } from './rfid-tags.store';

describe('RfidTagsStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/rfid-tags`;
  const entities: RfidTagApiModel[] = [
    { id: 'id-1', active: true },
    { id: 'id-2', active: false },
  ];

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

  const expectLoad = (): TestRequest => httpMock.expectOne((req) => req.method === 'GET' && req.url === url);

  async function load(): Promise<void> {
    trigger();
    expectLoad().flush(entities);
    await settle();
  }

  it('should load all entries on first use', async () => {
    await load();

    expect(store.entities()).toEqual(entities.map(toRfidTag));
    expect(store.isLoading()).toBe(false);
  });

  it('should reload the entries on request', async () => {
    await load();

    store.reloadRfidTags();
    trigger();
    expectLoad().flush([]);
    await settle();

    expect(store.entities()).toEqual([]);
  });

  it('should reload the entries after deleting one', async () => {
    await load();

    store.deleteRfidTag({ rfidTagId: 'id-1' });
    trigger();
    expect(store.deleteRfidTag.isPending()).toBe(true);
    httpMock.expectOne({ method: 'DELETE', url: `${url}/id-1` }).flush(null);
    trigger();

    expectLoad().flush([entities[1]]);
    await settle();
    expect(store.entities()).toEqual([toRfidTag(entities[1])]);
    expect(store.deleteRfidTag.isPending()).toBe(false);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    trigger();
    expectLoad().flush([{ id: 'id-1' }]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.entities()).toEqual([]);
  });

  it('should expose a load error', async () => {
    trigger();
    expectLoad().flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.entities()).toEqual([]);
  });
});
