import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidReader, RfidReaderType } from '../../../rfid-readers/rfid-reader.model';
import { CertificationRequirement, Tool, ToolArea } from '../../tool.model';
import { ToolRfidReaderAssignmentComponent } from './tool-rfid-reader-assignment.component';

describe('ToolRfidReaderAssignmentComponent', () => {
  const rfidReaders: RfidReader[] = [
    {
      id: 'reader-1',
      macAddress: 'aa',
      type: RfidReaderType.SwitchBox,
      name: 'Reader 1',
      socketName: '',
      createdAt: '2026-01-01T10:00:00Z',
    },
    {
      id: 'reader-2',
      macAddress: 'bb',
      type: RfidReaderType.SwitchBox,
      name: 'Reader 2',
      socketName: '',
      createdAt: '2026-01-01T10:00:00Z',
    },
    {
      id: 'reader-3',
      macAddress: 'cc',
      type: RfidReaderType.SwitchBox,
      name: 'Reader 3',
      socketName: '',
      createdAt: '2026-01-01T10:00:00Z',
    },
  ];

  function createTool(rfidReaderId: string | null = null): Tool {
    return {
      id: 'tool-1',
      name: 'Tool 1',
      description: '',
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

  let fixture: ComponentFixture<ToolRfidReaderAssignmentComponent>;
  let loader: HarnessLoader;
  let changed: Mock;
  let cleared: Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolRfidReaderAssignmentComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolRfidReaderAssignmentComponent);
    fixture.componentRef.setInput('tool', createTool('reader-1'));
    fixture.componentRef.setInput('rfidReaders', rfidReaders);
    fixture.componentRef.setInput('assignedRfidReaderIds', new Set(['reader-3']));
    changed = vi.fn();
    cleared = vi.fn();
    fixture.componentInstance.rfidReaderChanged.subscribe(changed);
    fixture.componentInstance.rfidReaderCleared.subscribe(cleared);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function actionButtons(): NodeListOf<HTMLButtonElement> {
    return fixture.nativeElement.querySelectorAll('.form-actions button');
  }

  it('should preselect the RFID reader of the tool', async () => {
    const select = await loader.getHarness(MatSelectHarness);

    expect(await select.getValueText()).toBe('Reader 1');
    expect(actionButtons().length).toBe(0);
  });

  it('should show save/cancel actions after selecting another RFID reader', async () => {
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: 'Reader 2' });

    expect(actionButtons().length).toBe(2);
  });

  it('should reset the selection when the tool input changes', async () => {
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: 'Reader 2' });

    fixture.componentRef.setInput('tool', createTool('reader-2'));
    await fixture.whenStable();

    expect(await select.getValueText()).toBe('Reader 2');
    expect(actionButtons().length).toBe(0);
  });

  it('should restore the selection of the tool on cancel', async () => {
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: 'Reader 2' });

    actionButtons()[0].click();
    await fixture.whenStable();

    expect(await select.getValueText()).toBe('Reader 1');
    expect(actionButtons().length).toBe(0);
  });

  async function selectReader(text: string | RegExp): Promise<void> {
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text });
    await fixture.whenStable();
  }

  it('should emit the change command on save', async () => {
    await selectReader('Reader 2');

    await (await loader.getHarness(MatButtonHarness.with({ selector: '[type="submit"]' }))).click();

    expect(changed).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', rfidReaderId: 'reader-2' });
  });

  it('should reject an RFID reader that is assigned to another tool', async () => {
    await selectReader(/Reader 3/);

    const field = await loader.getHarness(MatFormFieldHarness);
    expect(await field.getTextErrors()).toEqual(['tools.pages.toolDetails.messages.alreadyAssignedError']);
    expect(actionButtons().length).toBe(0);
  });

  it('should emit the clear command after confirmation', async () => {
    const dialog = fixture.debugElement.injector.get(MatDialog);
    vi.spyOn(dialog, 'open').mockReturnValue({ afterClosed: () => of(true) } as never);

    await (await loader.getHarness(MatButtonHarness.with({ selector: '.tool-rfid-reader-header button' }))).click();

    expect(cleared).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1' });
  });
});
