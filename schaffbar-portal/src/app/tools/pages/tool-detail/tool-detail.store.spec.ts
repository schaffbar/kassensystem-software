import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../../environments/environment';
import { ToolCertification } from '../../../users/shared/models/tool-certification.model';
import { CertificationRequirement, Tool, ToolArea } from '../../tool.model';
import { ToolDetailStore } from './tool-detail.store';

describe('ToolDetailStore', () => {
  const toolsUrl = `${environment.apiBaseUrl}/api/v1/tools`;
  const certificationsUrl = `${environment.apiBaseUrl}/api/v1/tool-certifications`;
  const tool: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
    description: '',
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Yellow,
    rfidReaderId: null,
    wlanRelaisType: null,
    ipAddress: null,
    httpStartCommand: null,
    onCommand: null,
    offCommand: null,
    instructors: [],
  };
  const certifications = [{ id: 'cert-1', customerId: 'user-1', toolId: 'tool-1' }] as ToolCertification[];

  let store: InstanceType<typeof ToolDetailStore>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), ToolDetailStore],
    });
    store = TestBed.inject(ToolDetailStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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
    await settle();
  }

  it('should not load anything without an id', () => {
    trigger();

    httpMock.expectNone(() => true);
    expect(store.tool()).toBeNull();
    expect(store.toolCertifications()).toEqual([]);
  });

  it('should load tool and certifications when the id is set', async () => {
    await loadTool();

    expect(store.tool()).toEqual(tool);
    expect(store.toolCertifications()).toEqual(certifications);
    expect(store.isLoading()).toBe(false);
  });

  it('should reload both on request', async () => {
    await loadTool();

    store.reloadTool();
    trigger();
    expectToolRequest().flush(tool);
    expectCertificationsRequest().flush([]);
    await settle();

    expect(store.toolCertifications()).toEqual([]);
  });

  it('should only reload the tool after adding instructors', async () => {
    await loadTool();

    store.addInstructors({ toolId: 'tool-1', instructorIds: ['user-1'] });
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

  it('should only reload the certifications after deleting one', async () => {
    await loadTool();

    store.deleteCertification({ customerId: 'user-1', toolId: 'tool-1' });
    trigger();
    const remove = httpMock.expectOne(`${certificationsUrl}/customers/user-1/tools/tool-1`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    trigger();

    expectCertificationsRequest().flush([]);
    await settle();
    expect(store.toolCertifications()).toEqual([]);
  });

  it('should expose a load error', async () => {
    store.setToolId('tool-1');
    trigger();
    expectToolRequest().flush('not found', { status: 404, statusText: 'Not Found' });
    expectCertificationsRequest().flush([]);
    await settle();

    expect(store.hasLoadError()).toBe(true);
    expect(store.tool()).toBeNull();
  });
});
