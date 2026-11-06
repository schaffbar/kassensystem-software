import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../../../environments/environment';
import { customerFixture } from '../../../shared/api/customers/customer.api-fixtures';
import { toolFixture } from '../../../shared/api/tools/tool.api-fixtures';
import { RfidReaderApiModel } from '../../../shared/api/rfid-readers/rfid-reader.api-model';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { ToolArea } from '../../../shared/models/tool-area';
import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { ToolCertificationApiModel } from '../../../shared/api/tool-certifications/tool-certification.api-model';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { toToolCertification } from '../../tool-certification.mapper';
import { toTool } from '../../tool.mapper';
import { ToolDetailStore } from './tool-detail.store';

describe('ToolDetailStore', () => {
  const toolsUrl = `${environment.apiBaseUrl}/api/v1/tools`;
  const certificationsUrl = `${environment.apiBaseUrl}/api/v1/tool-certifications`;
  const rfidReadersUrl = `${environment.apiBaseUrl}/api/v1/rfid-readers`;
  const customersUrl = `${environment.apiBaseUrl}/api/v1/customers`;
  const rfidReaders: RfidReaderApiModel[] = [
    {
      id: 'reader-1',
      macAddress: 'AA:BB',
      type: RfidReaderType.SwitchBox,
      name: 'Reader 1',
      socketName: null,
      createdAt: '2026-01-01T10:00:00Z',
    },
    {
      id: 'reader-2',
      macAddress: 'CC:DD',
      type: RfidReaderType.GateKeeperIn,
      name: 'Reader 2',
      socketName: null,
      createdAt: '2026-01-01T10:00:00Z',
    },
    {
      id: 'reader-3',
      macAddress: 'EE:FF',
      type: null,
      name: null,
      socketName: null,
      createdAt: '2026-01-01T10:00:00Z',
    },
  ];
  const tool = toolFixture();
  const certifications: ToolCertificationApiModel[] = [
    {
      id: 'cert-1',
      customerId: 'user-1',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Active,
      certifiedAt: '2026-01-01T10:00:00Z',
      certifiedBy: null,
    },
  ];

  let store: InstanceType<typeof ToolDetailStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), ToolDetailStore],
    });
    store = TestBed.inject(ToolDetailStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  // lets the resources react to signal changes and send their requests
  function trigger(): void {
    TestBed.tick();
  }

  // processes the flushed responses
  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  function expectToolRequest(): TestRequest {
    return httpMock.expectOne((req) => req.method === 'GET' && req.url === `${toolsUrl}/tool-1`);
  }

  function expectUsersRequest(): TestRequest {
    return httpMock.expectOne((req) => req.method === 'GET' && req.url === customersUrl);
  }

  function expectRfidReadersRequest(): TestRequest {
    return httpMock.expectOne((req) => req.method === 'GET' && req.url === rfidReadersUrl);
  }

  function expectCertificationsRequest(): TestRequest {
    return httpMock.expectOne(
      (req) => req.method === 'GET' && req.url === certificationsUrl && req.params.get('toolId') === 'tool-1',
    );
  }

  async function loadTool(): Promise<void> {
    store.setToolId('tool-1');
    trigger();
    expectToolRequest().flush(tool);
    expectCertificationsRequest().flush(certifications);
    expectRfidReadersRequest().flush(rfidReaders);
    expectUsersRequest().flush([customerFixture()]);
    await settle();
  }

  it('should load only the RFID readers and the users without an id', async () => {
    trigger();

    expectRfidReadersRequest().flush(rfidReaders);
    expectUsersRequest().flush([customerFixture()]);
    httpMock.expectNone(() => true);
    await settle();
    expect(store.tool()).toBeNull();
    expect(store.toolCertifications()).toEqual([]);
    // only the switch box – no other type, no reader without type
    expect(store.assignableRfidReaders()).toEqual([
      { id: 'reader-1', name: 'Reader 1', type: RfidReaderType.SwitchBox },
    ]);
    expect(store.users()).toEqual([{ id: 'user-1', firstName: 'Max', lastName: 'Mustermann' }]);
  });

  it('should load tool and certifications when the id is set', async () => {
    await loadTool();

    expect(store.tool()).toEqual(toTool(tool));
    expect(store.toolCertifications()).toEqual(certifications.map(toToolCertification));
    expect(store.isLoading()).toBe(false);
  });

  it('should split the users into instructors, candidates, certifiers and certifiable users', async () => {
    store.setToolId('tool-1');
    trigger();
    expectToolRequest().flush({ ...tool, instructors: ['user-2'] });
    // user-1 is certified already
    expectCertificationsRequest().flush(certifications);
    expectRfidReadersRequest().flush(rfidReaders);
    expectUsersRequest().flush([
      customerFixture(),
      customerFixture({ id: 'user-2', firstName: 'Erika' }),
      customerFixture({ id: 'user-3', firstName: 'Paul' }),
    ]);
    await settle();

    expect(store.instructors().map((user) => user.id)).toEqual(['user-2']);
    expect(store.instructorCandidates().map((user) => user.id)).toEqual(['user-1', 'user-3']);
    expect(store.certifiers().map((user) => user.id)).toEqual(['user-2']);
    expect(store.certifiableUsers().map((user) => user.id)).toEqual(['user-3']);
  });

  it('should reload both on request', async () => {
    await loadTool();

    store.reloadTool();
    trigger();
    expectToolRequest().flush(tool);
    expectCertificationsRequest().flush([]);
    expectRfidReadersRequest().flush(rfidReaders);
    expectUsersRequest().flush([customerFixture()]);
    await settle();

    expect(store.toolCertifications()).toEqual([]);
  });

  it('should only reload the tool after adding instructors', async () => {
    await loadTool();

    store.addToolInstructors({ toolId: 'tool-1', instructorIds: ['user-1'] });
    trigger();
    const add = httpMock.expectOne(`${toolsUrl}/tool-1/instructors`);
    expect(add.request.method).toBe('POST');
    expect(add.request.body).toEqual({ instructorIds: ['user-1'] });
    add.flush(null);
    trigger();

    expectToolRequest().flush({ ...tool, instructors: ['user-1'] });
    await settle();
    expect(store.tool()?.instructors).toEqual(['user-1']);
  });

  it('should send the tool fields without the id when updating', async () => {
    await loadTool();

    store.updateTool({
      toolId: 'tool-1',
      name: 'Bandsäge groß',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    });
    trigger();
    const update = httpMock.expectOne(`${toolsUrl}/tool-1`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual({
      name: 'Bandsäge groß',
      description: null,
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    });
    update.flush(null);
    trigger();

    expectToolRequest().flush(tool);
    await settle();
  });

  it('should assign the RFID reader via the path and set the WLAN relais via the body', async () => {
    await loadTool();

    store.changeRfidReader({ toolId: 'tool-1', rfidReaderId: 'reader-1' });
    trigger();
    const assign = httpMock.expectOne(`${toolsUrl}/tool-1/rfid-reader/reader-1`);
    expect(assign.request.method).toBe('PUT');
    expect(assign.request.body).toEqual({});
    assign.flush(null);
    trigger();
    expectToolRequest().flush(tool);
    await settle();

    store.setWlanRelais({ toolId: 'tool-1', wlanRelaisType: WlanRelaisType.Shelly1, ipAddress: '192.168.0.10' });
    trigger();
    const wlanRelais = httpMock.expectOne(`${toolsUrl}/tool-1/wlan-relais`);
    expect(wlanRelais.request.method).toBe('PUT');
    expect(wlanRelais.request.body).toEqual({ wlanRelaisType: WlanRelaisType.Shelly1, ipAddress: '192.168.0.10' });
    wlanRelais.flush(null);
    trigger();
    expectToolRequest().flush(tool);
    await settle();
  });

  it('should send the instructor ids as body of the DELETE when removing instructors', async () => {
    await loadTool();

    store.removeToolInstructors({ toolId: 'tool-1', instructorIds: ['user-1'] });
    trigger();
    const remove = httpMock.expectOne(`${toolsUrl}/tool-1/instructors`);
    expect(remove.request.method).toBe('DELETE');
    expect(remove.request.body).toEqual({ instructorIds: ['user-1'] });
    remove.flush(null);
    trigger();

    expectToolRequest().flush(tool);
    await settle();
  });

  it('should only reload the certifications after certifying several users', async () => {
    await loadTool();

    store.batchCreateToolCertifications({ userIds: ['user-1', 'user-2'], toolId: 'tool-1', certifiedBy: 'user-3' });
    trigger();
    const batch = httpMock.expectOne(`${certificationsUrl}/batch`);
    expect(batch.request.method).toBe('POST');
    expect(batch.request.body).toEqual({ customerIds: ['user-1', 'user-2'], toolId: 'tool-1', certifiedBy: 'user-3' });
    batch.flush({});
    trigger();

    expectCertificationsRequest().flush(certifications);
    await settle();
    expect(store.toolCertifications()).toEqual(certifications.map(toToolCertification));
  });

  it('should only reload the certifications after deleting one', async () => {
    await loadTool();

    store.deleteToolCertification({ userId: 'user-1', toolId: 'tool-1' });
    trigger();
    const remove = httpMock.expectOne(`${certificationsUrl}/customers/user-1/tools/tool-1`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    trigger();

    expectCertificationsRequest().flush([]);
    await settle();
    expect(store.toolCertifications()).toEqual([]);
  });

  it('should expose a load error for a response that does not match the schema', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    store.setToolId('tool-1');
    trigger();
    expectToolRequest().flush({ ...tool, area: 'GARTEN' });
    expectCertificationsRequest().flush(certifications);
    expectRfidReadersRequest().flush(rfidReaders);
    expectUsersRequest().flush([customerFixture()]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.tool()).toBeNull();
  });

  it('should expose a load error', async () => {
    store.setToolId('tool-1');
    trigger();
    expectToolRequest().flush('not found', { status: 404, statusText: 'Not Found' });
    expectCertificationsRequest().flush([]);
    expectRfidReadersRequest().flush([]);
    expectUsersRequest().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.tool()).toBeNull();
  });
});
