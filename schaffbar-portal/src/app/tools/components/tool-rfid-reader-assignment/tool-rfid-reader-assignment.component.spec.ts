import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { RfidReader, RfidReaderType } from '../../../rfid-readers/rfid-reader.model';
import { CertificationRequirement, Tool, ToolArea } from '../../tool.model';
import { ToolRfidReaderAssignmentComponent } from './tool-rfid-reader-assignment.component';

describe('ToolRfidReaderAssignmentComponent', () => {
  const rfidReaders: RfidReader[] = [
    { id: 'reader-1', macAddress: 'aa', type: RfidReaderType.SwitchBox, name: 'Reader 1', socketName: '' },
    { id: 'reader-2', macAddress: 'bb', type: RfidReaderType.SwitchBox, name: 'Reader 2', socketName: '' },
    { id: 'reader-3', macAddress: 'cc', type: RfidReaderType.SwitchBox, name: 'Reader 3', socketName: '' },
  ];

  function createTool(rfidReaderId?: string): Tool {
    return {
      id: 'tool-1',
      name: 'Tool 1',
      description: '',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Yellow,
      rfidReaderId,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
  }

  let fixture: ComponentFixture<ToolRfidReaderAssignmentComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolRfidReaderAssignmentComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolRfidReaderAssignmentComponent);
    fixture.componentRef.setInput('tool', createTool('reader-1'));
    fixture.componentRef.setInput('rfidReaders', rfidReaders);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function actionButtons(): NodeListOf<HTMLButtonElement> {
    return fixture.nativeElement.querySelectorAll('.tool-rfid-reader-actions button');
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

    fixture.componentRef.setInput('tool', createTool('reader-3'));
    await fixture.whenStable();

    expect(await select.getValueText()).toBe('Reader 3');
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
});
