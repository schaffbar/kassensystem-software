import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../../../environments/environment';
import { toolFixture } from '../../../shared/api/tools/tool.api-fixtures';
import { RfidReaderApiModel } from '../../../shared/api/rfid-readers/rfid-reader.api-model';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { toRfidReader } from '../../rfid-reader.mapper';
import { ToolApiModel } from '../../../shared/api/tools/tool.api-model';
import { RfidReaderDetailStore } from './rfid-reader-detail.store';

describe('RfidReaderDetailStore', () => {
  const url = `${environment.apiBaseUrl}/api/v1/rfid-readers`;
  const toolsUrl = `${environment.apiBaseUrl}/api/v1/tools`;
  const reader: RfidReaderApiModel = {
    id: 'reader-1',
    macAddress: 'AA:BB',
    type: RfidReaderType.SwitchBox,
    name: 'Reader 1',
    socketName: '',
    createdAt: '2026-01-01T10:00:00Z',
  };

  let store: InstanceType<typeof RfidReaderDetailStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        RfidReaderDetailStore,
      ],
    });
    store = TestBed.inject(RfidReaderDetailStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  // lets the resource react to signal changes and send its request (whenStable() would wait for the open request)
  function trigger(): void {
    TestBed.tick();
  }

  // processes the flushed response
  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  const expectToolsOf = () =>
    httpMock.expectOne((req) => req.url === toolsUrl && req.params.get('rfidReaderId') === 'reader-1');

  async function loadReader(apiModel: RfidReaderApiModel = reader, tools: ToolApiModel[] = []): Promise<void> {
    store.setRfidReaderId('reader-1');
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush(apiModel);
    expectToolsOf().flush(tools);
    await settle();
  }

  it('should not load anything without an id', async () => {
    trigger();

    httpMock.expectNone(() => true);
    expect(store.rfidReader()).toBeNull();
  });

  it('should load the reader when the id is set', async () => {
    await loadReader();

    expect(store.rfidReader()).toEqual({ ...toRfidReader(reader), assignedTool: null });
    expect(store.isLoading()).toBe(false);
  });

  it('should add the tool the switch box is assigned to', async () => {
    await loadReader(reader, [toolFixture({ id: 'tool-1', name: 'Bandsäge', rfidReaderId: 'reader-1' })]);

    expect(store.rfidReader()?.assignedTool).toEqual({ id: 'tool-1', name: 'Bandsäge' });
  });

  it('should reload the reader on request', async () => {
    await loadReader();

    store.reloadRfidReader();
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush({ ...reader, name: 'Reader 1 (neu)' });
    expectToolsOf().flush([]);
    await settle();

    expect(store.rfidReader()?.name).toBe('Reader 1 (neu)');
  });

  it('should reload the reader after an update', async () => {
    await loadReader();

    store.updateRfidReader({ rfidReaderId: 'reader-1', name: 'Neuer Name' });
    trigger();
    const update = httpMock.expectOne(`${url}/reader-1`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual({ name: 'Neuer Name', socketName: null });
    expect(store.updateRfidReader.isPending()).toBe(true);
    update.flush(null);
    trigger();

    httpMock.expectOne((req) => req.method === 'GET' && req.url === `${url}/reader-1`).flush(reader);
    await settle();
    expect(store.updateRfidReader.isPending()).toBe(false);
  });

  it('should send only the type when changing it', async () => {
    await loadReader();

    store.changeRfidReaderType({ rfidReaderId: 'reader-1', type: RfidReaderType.GateKeeperIn });
    trigger();
    const change = httpMock.expectOne(`${url}/reader-1/type`);
    expect(change.request.method).toBe('PATCH');
    expect(change.request.body).toEqual({ type: RfidReaderType.GateKeeperIn });
    change.flush(null);
    trigger();

    httpMock.expectOne(`${url}/reader-1`).flush({ ...reader, type: RfidReaderType.GateKeeperIn });
    await settle();
    expect(store.rfidReader()?.type).toBe(RfidReaderType.GateKeeperIn);
  });

  it('should go back to the list after deleting the reader', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await loadReader();

    store.deleteRfidReader({ rfidReaderId: 'reader-1' });
    trigger();
    const deletion = httpMock.expectOne(`${url}/reader-1`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    await settle();

    expect(navigate).toHaveBeenCalledExactlyOnceWith(['rfid-readers']);
  });

  it('should stay on the page when deleting fails', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await loadReader();

    store.deleteRfidReader({ rfidReaderId: 'reader-1' });
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush(null, { status: 409, statusText: 'Conflict' });
    await settle();

    expect(navigate).not.toHaveBeenCalled();
  });

  describe('canChangeRfidReaderType', () => {
    it('should not allow it for a switch box with an assigned tool', async () => {
      await loadReader();

      const canChange = firstValueFrom(store.canChangeRfidReaderType());
      expectToolsOf().flush([toolFixture({ rfidReaderId: 'reader-1' })]);

      expect(await canChange).toBe(false);
    });

    it('should allow it for a switch box without tools', async () => {
      await loadReader();

      const canChange = firstValueFrom(store.canChangeRfidReaderType());
      expectToolsOf().flush([]);

      expect(await canChange).toBe(true);
    });

    it('should allow it for any other type without asking the backend', async () => {
      await loadReader({ ...reader, type: RfidReaderType.GateKeeperIn });

      expect(await firstValueFrom(store.canChangeRfidReaderType())).toBe(true);
      httpMock.expectNone(() => true);
    });

    it('should not allow it without a loaded reader', async () => {
      expect(await firstValueFrom(store.canChangeRfidReaderType())).toBe(false);
    });
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    store.setRfidReaderId('reader-1');
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush({ ...reader, createdAt: 'gestern' });
    expectToolsOf().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.rfidReader()).toBeNull();
  });

  it('should expose a load error', async () => {
    store.setRfidReaderId('reader-1');
    trigger();
    httpMock.expectOne(`${url}/reader-1`).flush('not found', { status: 404, statusText: 'Not Found' });
    expectToolsOf().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.rfidReader()).toBeNull();
  });
});
