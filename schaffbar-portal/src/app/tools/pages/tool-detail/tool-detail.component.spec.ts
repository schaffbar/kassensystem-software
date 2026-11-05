import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatTabGroupHarness } from '@angular/material/tabs/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { ToolArea } from '../../../shared/models/tool-area';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { ToolCertificationsComponent } from '../../components/tool-certifications/tool-certifications.component';
import { ToolRfidReaderAssignmentComponent } from '../../components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component';
import { RfidReader } from '../../rfid-reader.model';
import { ToolCertification } from '../../tool-certification.model';
import { Tool } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { User } from '../../user.model';
import { ToolDetailComponent } from './tool-detail.component';
import { ToolDetailStore } from './tool-detail.store';

describe('ToolDetailComponent', () => {
  function createTool(id: string, rfidReaderId: string | null): Tool {
    return {
      id,
      name: `Tool ${id}`,
      description: null,
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Yellow,
      rfidReaderId,
      wlanRelaisType: null,
      ipAddress: null,
      httpStartCommand: null,
      onCommand: null,
      offCommand: null,
      instructors: [],
    };
  }

  const tool = createTool('tool-1', 'reader-1');
  const users: User[] = [{ id: 'user-1', firstName: 'Max', lastName: 'Mustermann' }];
  const certifications: ToolCertification[] = [
    {
      id: 'cert-1',
      customerId: 'user-1',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Active,
      certifiedAt: new Date('2026-01-01T10:00:00Z'),
      certifiedBy: null,
    },
    {
      id: 'cert-2',
      customerId: 'user-deleted',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Paused,
      certifiedAt: new Date('2026-02-01T10:00:00Z'),
      certifiedBy: null,
    },
  ];
  const rfidReaders: RfidReader[] = [
    { id: 'reader-1', name: 'Reader 1', type: RfidReaderType.SwitchBox },
    { id: 'reader-2', name: 'Reader 2', type: RfidReaderType.SwitchBox },
    { id: 'reader-3', name: 'Reader 3', type: RfidReaderType.SwitchBox },
  ];

  let fixture: ComponentFixture<ToolDetailComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    const detailsStoreMock = {
      isLoading: signal(false),
      hasLoadError: signal(false),
      tool: signal(tool),
      toolCertifications: signal(certifications),
      users: signal(users),
      assignableRfidReaders: signal(rfidReaders),
      instructors: signal([]),
      instructorCandidates: signal(users),
      certifiers: signal([]),
      certifiableUsers: signal([]),
      setToolId: vi.fn(),
      reloadTool: vi.fn(),
    };
    const toolsStoreMock = {
      // reader-1 belongs to this tool, reader-2 to another one
      entities: signal([tool, createTool('tool-2', 'reader-2'), createTool('tool-3', null)]),
      reloadTools: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ToolDetailComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        // the content of a tab is attached when its animation ends
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        { provide: ToolsStore, useValue: toolsStoreMock },
      ],
    })
      .overrideComponent(ToolDetailComponent, {
        set: { providers: [{ provide: ToolDetailStore, useValue: detailsStoreMock }] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ToolDetailComponent);
    fixture.componentRef.setInput('id', 'tool-1');
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  async function selectTab(label: string): Promise<void> {
    await (await loader.getHarness(MatTabGroupHarness)).selectTab({ label });
    await fixture.whenStable();
  }

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should pass the certifications with the user names', async () => {
    await selectTab('tools.tabGroups.toolDetails.certificationsTab');

    expect(child(ToolCertificationsComponent).certifications()).toEqual([
      {
        customerId: 'user-1',
        toolId: 'tool-1',
        userName: 'Max Mustermann',
        status: ToolCertificationStatus.Active,
        certifiedAt: new Date('2026-01-01T10:00:00Z'),
      },
      {
        customerId: 'user-deleted',
        toolId: 'tool-1',
        userName: null,
        status: ToolCertificationStatus.Paused,
        certifiedAt: new Date('2026-02-01T10:00:00Z'),
      },
    ]);
  });

  it('should mark only the RFID readers of other tools as assigned', async () => {
    await selectTab('tools.tabGroups.toolDetails.configurationTab');

    expect(child(ToolRfidReaderAssignmentComponent).rfidReaders()).toEqual([
      { id: 'reader-1', name: 'Reader 1', isAssignedToOtherTool: false },
      { id: 'reader-2', name: 'Reader 2', isAssignedToOtherTool: true },
      { id: 'reader-3', name: 'Reader 3', isAssignedToOtherTool: false },
    ]);
  });
});
