import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement } from '../../../../shared/models/certification-requirement';
import { RfidReaderType } from '../../../../shared/models/rfid-reader-type';
import { ToolArea } from '../../../../shared/models/tool-area';
import { ToolRfidReaderAssignmentComponent } from '../../../components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component';
import { ToolWlanRelaisComponent } from '../../../components/tool-wlan-relais/tool-wlan-relais.component';
import { RfidReader } from '../../../rfid-reader.model';
import { Tool } from '../../../tool.model';
import { ToolsStore } from '../../../tools.store';
import { ToolDetailStore } from '../tool-detail.store';
import { ToolDetailTabConfigurationComponent } from './tool-detail-tab-configuration.component';

describe('ToolDetailTabConfigurationComponent', () => {
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
  const rfidReaders: RfidReader[] = [
    { id: 'reader-1', macAddress: 'AA:BB', name: 'Reader 1', type: RfidReaderType.SwitchBox },
    { id: 'reader-2', macAddress: 'CC:DD', name: 'Reader 2', type: RfidReaderType.SwitchBox },
    { id: 'reader-3', macAddress: 'EE:FF', name: null, type: RfidReaderType.SwitchBox },
  ];

  let fixture: ComponentFixture<ToolDetailTabConfigurationComponent>;
  let clearWlanRelais: Mock;

  beforeEach(async () => {
    clearWlanRelais = vi.fn();
    const detailsStoreMock = {
      tool: signal(tool),
      assignableRfidReaders: signal(rfidReaders),
      clearWlanRelais,
    };
    // reader-1 belongs to this tool, reader-2 to another one
    const toolsStoreMock = { tools: signal([tool, createTool('tool-2', 'reader-2'), createTool('tool-3', null)]) };

    await TestBed.configureTestingModule({
      imports: [ToolDetailTabConfigurationComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ToolDetailStore, useValue: detailsStoreMock },
        { provide: ToolsStore, useValue: toolsStoreMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDetailTabConfigurationComponent);
    await fixture.whenStable();
  });

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should mark only the RFID readers of other tools as assigned and label a reader without name by its MAC', () => {
    expect(child(ToolRfidReaderAssignmentComponent).rfidReaders()).toEqual([
      { id: 'reader-1', label: 'Reader 1', isAssignedToOtherTool: false },
      { id: 'reader-2', label: 'Reader 2', isAssignedToOtherTool: true },
      { id: 'reader-3', label: 'EE:FF', isAssignedToOtherTool: false },
    ]);
  });

  it('should execute the clear command of the WLAN relais', () => {
    child(ToolWlanRelaisComponent).wlanRelaisCleared.emit({ toolId: 'tool-1' });

    expect(clearWlanRelais).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1' });
  });
});
