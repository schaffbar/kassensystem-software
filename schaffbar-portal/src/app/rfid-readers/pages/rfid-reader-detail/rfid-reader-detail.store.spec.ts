import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../../environments/environment';
import { RfidReader, RfidReaderType } from '../../rfid-reader.model';
import { RfidReaderDetailStore } from './rfid-reader-detail.store';

describe('RfidReaderDetailStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/rfid-readers`;
  const reader: RfidReader = {
    id: 'reader-1',
    macAddress: 'AA:BB',
    type: RfidReaderType.SwitchBox,
    name: 'Reader 1',
    socketName: '',
  };

  let store: InstanceType<typeof RfidReaderDetailStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        RfidReaderDetailStore,
      ],
    });
    store = TestBed.inject(RfidReaderDetailStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // lets the resource react to signal changes and send its request (whenStable() would wait for the open request)
  function trigger(): void {
    TestBed.tick();
  }

  // processes the flushed response
  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  async function loadReader(): Promise<void> {
    store.setRfidReaderId('reader-1');
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush(reader);
    await settle();
  }

  it('should not load anything without an id', async () => {
    trigger();

    httpMock.expectNone(() => true);
    expect(store.rfidReader()).toBeNull();
  });

  it('should load the reader when the id is set', async () => {
    await loadReader();

    expect(store.rfidReader()).toEqual(reader);
    expect(store.isLoading()).toBe(false);
  });

  it('should reload the reader on request', async () => {
    await loadReader();

    store.reloadRfidReader();
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush({ ...reader, name: 'Reader 1 (neu)' });
    await settle();

    expect(store.rfidReader()?.name).toBe('Reader 1 (neu)');
  });

  it('should reload the reader after an update', async () => {
    await loadReader();

    store.updateRfidReader({ id: 'reader-1', name: 'Neuer Name' });
    trigger();
    const update = httpMock.expectOne(`${url}/reader-1`);
    expect(update.request.method).toBe('PUT');
    expect(store.updateRfidReader.isPending()).toBe(true);
    update.flush(null);
    trigger();

    httpMock.expectOne((req) => req.method === 'GET' && req.url === `${url}/reader-1`).flush(reader);
    await settle();
    expect(store.updateRfidReader.isPending()).toBe(false);
  });

  it('should expose a load error', async () => {
    store.setRfidReaderId('reader-1');
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush('not found', { status: 404, statusText: 'Not Found' });
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.rfidReader()).toBeNull();
  });
});
