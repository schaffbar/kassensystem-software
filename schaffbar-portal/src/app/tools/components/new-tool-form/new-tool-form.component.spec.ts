import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { ExistingToolViewModel, NewToolDialogData, NewToolFormComponent } from './new-tool-form.component';

describe('NewToolFormComponent', () => {
  let fixture: ComponentFixture<NewToolFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };
  let existingTools: WritableSignal<ExistingToolViewModel[]>;

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };
    existingTools = signal([{ id: 'tool-1', name: 'Bandsäge' }]);
    const data: NewToolDialogData = { existingTools };

    await TestBed.configureTestingModule({
      imports: [NewToolFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NewToolFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  async function setName(name: string): Promise<void> {
    const input = await loader.getHarness(MatInputHarness.with({ selector: '#name' }));
    await input.setValue(name);
  }

  async function selectOption(index: number, text: string): Promise<void> {
    const selects = await loader.getAllHarnesses(MatSelectHarness);
    await selects[index].open();
    await selects[index].clickOptions({ text });
  }

  async function clickSave(): Promise<void> {
    const save = await loader.getHarness(MatButtonHarness.with({ text: 'shared.actions.save' }));
    await save.click();
    await fixture.whenStable();
  }

  async function nameErrors(): Promise<string[]> {
    const nameField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /name/ }));
    return nameField.getTextErrors();
  }

  it('should show required errors and not close when saving an empty form', async () => {
    await clickSave();

    const formFields = await loader.getAllHarnesses(MatFormFieldHarness);
    const errors = await Promise.all(formFields.map((field) => field.getTextErrors()));
    expect(errors.flat().filter((error) => error === 'shared.errors.required').length).toBe(3);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should reject a name that already exists (case-insensitive)', async () => {
    await setName('BANDSÄGE');
    await clickSave();

    expect(await nameErrors()).toEqual(['tools.dialogs.addTool.errors.nameExists']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should re-validate the name when the tool list changes', async () => {
    await setName('Drechselbank');
    await clickSave();
    expect(await nameErrors()).not.toContain('tools.dialogs.addTool.errors.nameExists');

    existingTools.set([...existingTools(), { id: 'tool-2', name: 'Drechselbank' }]);
    await fixture.whenStable();

    expect(await nameErrors()).toEqual(['tools.dialogs.addTool.errors.nameExists']);
  });

  it('should close the dialog with the create command', async () => {
    await setName('Drechselbank');
    await selectOption(0, 'tools.areas.HOLZ');
    await selectOption(1, 'tools.certificationRequirements.RED');

    await clickSave();

    expect(dialogRef.close).toHaveBeenCalledWith({
      name: 'Drechselbank',
      description: undefined,
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    });
  });

  it('should only close the dialog on cancel without saving', async () => {
    await setName('Drechselbank');

    const cancel = await loader.getHarness(MatButtonHarness.with({ text: 'shared.actions.cancel' }));
    await cancel.click();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
