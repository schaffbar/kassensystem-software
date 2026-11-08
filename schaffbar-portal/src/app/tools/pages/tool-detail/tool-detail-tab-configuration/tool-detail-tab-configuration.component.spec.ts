import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
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
import { ToolListItem } from '../../../tool.model';
import { ToolsStore } from '../../../tools.store';
import { ToolDetailStore } from '../tool-detail.store';
import { ToolDetailTabConfigurationComponent } from './tool-detail-tab-configuration.component';

describe('ToolDetailTabConfigurationComponent', () => {
  function createTool(id: string, rfidReaderId: string | null, usedBy: string | null = null): ToolListItem {
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
      usedBy,
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
  let tools: WritableSignal<ToolListItem[]>;

  beforeEach(async () => {
    clearWlanRelais = vi.fn();
    const detailsStoreMock = {
      tool: signal(tool),
      assignableRfidReaders: signal(rfidReaders),
      clearWlanRelais,
    };
    // reader-1 belongs to this tool, reader-2 to another one
    tools = signal([tool, createTool('tool-2', 'reader-2'), createTool('tool-3', null)]);
    const toolsStoreMock = { tools };

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

  it('should offer the free switch boxes and the own one, but none of another tool', () => {
    expect(child(ToolRfidReaderAssignmentComponent).rfidReaders()).toEqual([
      { id: 'reader-1', name: 'Reader 1', macAddress: 'AA:BB' },
      { id: 'reader-3', name: null, macAddress: 'EE:FF' },
    ]);
  });

  it('should allow to disconnect the switch box only while nobody uses the tool', async () => {
    expect(child(ToolRfidReaderAssignmentComponent).toolRfidReaderAssignment().canClearRfidReader).toBe(true);

    tools.set([createTool('tool-1', 'reader-1', 'Max Mustermann')]);
    await fixture.whenStable();

    expect(child(ToolRfidReaderAssignmentComponent).toolRfidReaderAssignment().canClearRfidReader).toBe(false);
  });

  it('should execute the clear command of the WLAN relais', () => {
    child(ToolWlanRelaisComponent).wlanRelaisCleared.emit({ toolId: 'tool-1' });

    expect(clearWlanRelais).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1' });
  });
});
