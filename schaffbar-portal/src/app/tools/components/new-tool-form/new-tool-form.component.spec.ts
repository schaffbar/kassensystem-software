import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';

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

  async function choose(group: 'area' | 'requirement', label: RegExp): Promise<void> {
    const radioGroup = await loader.getHarness(MatRadioGroupHarness.with({ selector: `.${group}-options` }));
    await radioGroup.checkRadioButton({ label });
  }

  async function clickSave(): Promise<void> {
    const save = await loader.getHarness(MatButtonHarness.with({ text: 'tools.dialogs.addTool.confirm' }));
    await save.click();
    await fixture.whenStable();
  }

  async function nameErrors(): Promise<string[]> {
    const nameField = await loader.getHarness(MatFormFieldHarness.with({ selector: ':has(#name)' }));
    return nameField.getTextErrors();
  }

  it('should show the dialog header with title and hint', () => {
    expect(fixture.nativeElement.querySelector('schbar-form-dialog-header h2').textContent.trim()).toBe(
      'tools.dialogs.addTool.title',
    );
    expect(fixture.nativeElement.querySelector('schbar-form-dialog-header .subtitle').textContent.trim()).toBe(
      'tools.dialogs.addTool.subtitle',
    );
  });

  it('should show required errors and not close when saving an empty form', async () => {
    await clickSave();

    expect(await nameErrors()).toEqual(['shared.errors.required']);
    const choiceErrors = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('.choice-error')).map((error) =>
      error.textContent?.trim(),
    );
    expect(choiceErrors).toEqual(['shared.errors.required', 'shared.errors.required']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should offer the areas and the requirements with their description as cards', () => {
    const cards = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('.requirement-options .radio-card'));
    expect(cards.map((card) => card.querySelector('.radio-card-description')?.textContent?.trim())).toEqual([
      'tools.certificationRequirementDescriptions.YELLOW',
      'tools.certificationRequirementDescriptions.RED',
    ]);
    expect(fixture.nativeElement.querySelectorAll('.area-options .radio-card')).toHaveLength(4);
  });

  it('should limit the description to what the database holds and count the characters', async () => {
    const description = await loader.getHarness(MatInputHarness.with({ selector: '#description' }));
    await description.setValue('x'.repeat(256));
    await clickSave();

    const field = await loader.getHarness(MatFormFieldHarness.with({ selector: ':has(#description)' }));
    expect(await field.getTextErrors()).toEqual(['shared.errors.maxLength']);
    expect(dialogRef.close).not.toHaveBeenCalled();

    await description.setValue('Hobelbank');
    await fixture.whenStable();
    expect(await field.getTextHints()).toEqual(['9 / 255']);
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
    await choose('area', /tools\.areas\.HOLZ/);
    await choose('requirement', /tools\.certificationRequirements\.RED/);

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
