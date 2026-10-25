import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { describe, expect, it, vi, type Mock } from 'vitest';

import { RfidReader, RfidReaderType } from '../../rfid-reader.model';
import { ChangeRfidReaderTypeFormComponent } from './change-rfid-reader-type-form.component';

describe('ChangeRfidReaderTypeFormComponent', () => {
  let fixture: ComponentFixture<ChangeRfidReaderTypeFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };

  async function setup(type: RfidReaderType | null): Promise<void> {
    const rfidReader = { id: 'reader-1', macAddress: 'aa', type, name: 'Reader 1', socketName: '' } as RfidReader;
    dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ChangeRfidReaderTypeFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MAT_DIALOG_DATA, useValue: { rfidReader } },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChangeRfidReaderTypeFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  }

  async function clickSave(): Promise<void> {
    const save = await loader.getHarness(MatButtonHarness.with({ text: 'shared.dialogs.actions.edit.confirm' }));
    await save.click();
    await fixture.whenStable();
  }

  it('should preselect the current type', async () => {
    await setup(RfidReaderType.SwitchBox);
    const select = await loader.getHarness(MatSelectHarness);

    expect(await select.getValueText()).toBe('rfidReaders.types.switch_box');
  });

  it('should close the dialog with the selected type', async () => {
    await setup(RfidReaderType.SwitchBox);
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: 'rfidReaders.types.gate_keeper_in' });

    await clickSave();

    expect(dialogRef.close).toHaveBeenCalledWith({ id: 'reader-1', type: RfidReaderType.GateKeeperIn });
  });

  it('should show a required error and not close when no type is selected', async () => {
    await setup(null);

    await clickSave();

    const formField = await loader.getHarness(MatFormFieldHarness);
    expect(await formField.getTextErrors()).toEqual(['shared.errors.required']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should save when the form is submitted (e.g. Enter)', async () => {
    await setup(RfidReaderType.SwitchBox);

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({ id: 'reader-1', type: RfidReaderType.SwitchBox });
  });

  it('should only close the dialog on cancel without saving', async () => {
    await setup(RfidReaderType.SwitchBox);

    const cancel = await loader.getHarness(MatButtonHarness.with({ text: 'shared.dialogs.actions.edit.cancel' }));
    await cancel.click();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
