import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement } from '../../../../shared/models/certification-requirement';
import { ToolCertificationStatus } from '../../../../shared/models/tool-certification-status';
import { ToolArea } from '../../../../shared/models/tool-area';
import { WlanRelaisType } from '../../../../shared/models/wlan-relais-type';
import { AddToolInstructorsFormComponent } from '../../../components/add-tool-instructors-form/add-tool-instructors-form.component';
import { ToolDetailInfoComponent } from '../../../components/tool-detail-info/tool-detail-info.component';
import { ToolHardwareSummaryComponent } from '../../../components/tool-hardware-summary/tool-hardware-summary.component';
import { ToolInstructorsComponent } from '../../../components/tool-instructors/tool-instructors.component';
import { UpdateToolFormComponent } from '../../../components/update-tool-form/update-tool-form.component';
import { AddToolInstructorsCommand, Tool, UpdateToolCommand } from '../../../tool.model';
import { ToolsStore } from '../../../tools.store';
import { InstructorCandidate, ToolInstructor } from '../../../user.model';
import { ToolDetailStore } from '../tool-detail.store';
import { ToolDetailTabInfoComponent } from './tool-detail-tab-info.component';

describe('ToolDetailTabInfoComponent', () => {
  const tool: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
    description: 'Schnitthöhe 30 cm',
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Yellow,
    rfidReaderId: 'reader-1',
    wlanRelaisType: WlanRelaisType.Shelly1,
    ipAddress: '192.168.1.56',
    httpStartCommand: null,
    onCommand: null,
    offCommand: null,
    instructors: ['user-1'],
  };
  const instructors: ToolInstructor[] = [
    { id: 'user-1', firstName: 'Max', lastName: 'Mustermann', certificationsGiven: 3 },
  ];
  const candidates: InstructorCandidate[] = [
    {
      id: 'user-2',
      firstName: 'Erika',
      lastName: 'Musterfrau',
      certification: { status: ToolCertificationStatus.Active, certifiedAt: new Date('2026-01-01T10:00:00Z') },
    },
  ];
  const tools = signal([{ id: 'tool-1', name: 'Bandsäge' }]);

  let fixture: ComponentFixture<ToolDetailTabInfoComponent>;
  let detailsStoreMock: {
    tool: ReturnType<typeof signal<Tool>>;
    instructors: ReturnType<typeof signal<ToolInstructor[]>>;
    instructorCandidates: ReturnType<typeof signal<InstructorCandidate[]>>;
    activeCertificationsCount: ReturnType<typeof signal<number>>;
    assignedRfidReader: ReturnType<typeof signal<unknown>>;
    updateTool: Mock;
    addToolInstructors: Mock;
    removeToolInstructors: Mock;
  };
  let openDialog: Mock;

  beforeEach(async () => {
    openDialog = vi.fn();
    detailsStoreMock = {
      tool: signal(tool),
      instructors: signal(instructors),
      instructorCandidates: signal(candidates),
      activeCertificationsCount: signal(12),
      assignedRfidReader: signal({ id: 'reader-1', name: 'Switch Bandsäge', macAddress: 'AA:BB', type: null }),
      updateTool: vi.fn(),
      addToolInstructors: vi.fn(),
      removeToolInstructors: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ToolDetailTabInfoComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: ToolDetailStore, useValue: detailsStoreMock },
        { provide: ToolsStore, useValue: { tools } },
        { provide: MatDialog, useValue: { open: openDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDetailTabInfoComponent);
    await fixture.whenStable();
  });

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should pass the tool with the number of active certifications to the info', () => {
    expect(child(ToolDetailInfoComponent).toolDetailInfo()).toEqual(
      expect.objectContaining({ description: 'Schnitthöhe 30 cm', activeCertificationsCount: 12 }),
    );
  });

  it('should pass the switch box and the WLAN relais to the hardware summary', () => {
    expect(child(ToolHardwareSummaryComponent).toolHardwareSummary()).toEqual({
      rfidReader: { id: 'reader-1', name: 'Switch Bandsäge', macAddress: 'AA:BB' },
      wlanRelaisType: WlanRelaisType.Shelly1,
      ipAddress: '192.168.1.56',
    });
  });

  it('should ask the page for the hardware tab', () => {
    const requested = vi.fn();
    fixture.componentInstance.hardwareConfigurationRequested.subscribe(requested);

    child(ToolHardwareSummaryComponent).configureRequested.emit();

    expect(requested).toHaveBeenCalledOnce();
  });

  it('should open the edit form with all tools for the name check and update the tool', () => {
    const command: UpdateToolCommand = {
      toolId: 'tool-1',
      name: 'Bandsäge',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    };
    openDialog.mockReturnValue({ afterClosed: () => of(command) });

    child(ToolDetailInfoComponent).editRequested.emit();

    expect(openDialog).toHaveBeenCalledWith(
      UpdateToolFormComponent,
      expect.objectContaining({ data: { tool, activeCertificationsCount: 12, existingTools: tools } }),
    );
    expect(detailsStoreMock.updateTool).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('should open the instructor form with the candidates and add the selected instructors', () => {
    const command: AddToolInstructorsCommand = { toolId: 'tool-1', instructorIds: ['user-2'] };
    openDialog.mockReturnValue({ afterClosed: () => of(command) });

    child(ToolInstructorsComponent).addRequested.emit();

    expect(openDialog).toHaveBeenCalledWith(
      AddToolInstructorsFormComponent,
      expect.objectContaining({ data: { toolId: 'tool-1', toolName: 'Bandsäge', candidates } }),
    );
    expect(detailsStoreMock.addToolInstructors).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('should not add instructors when the form is cancelled', () => {
    openDialog.mockReturnValue({ afterClosed: () => of(undefined) });

    child(ToolInstructorsComponent).addRequested.emit();

    expect(detailsStoreMock.addToolInstructors).not.toHaveBeenCalled();
  });

  it('should pass the instructors and execute their remove command', () => {
    const instructorsComponent = child(ToolInstructorsComponent);
    expect(instructorsComponent.instructors()).toBe(instructors);

    instructorsComponent.instructorsRemoved.emit({ toolId: 'tool-1', instructorIds: ['user-1'] });

    expect(detailsStoreMock.removeToolInstructors).toHaveBeenCalledExactlyOnceWith({
      toolId: 'tool-1',
      instructorIds: ['user-1'],
    });
  });
});
