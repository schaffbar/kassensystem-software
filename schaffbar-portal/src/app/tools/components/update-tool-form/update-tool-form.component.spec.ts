import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { UpdateToolDialogData, UpdateToolFormComponent } from './update-tool-form.component';

describe('UpdateToolFormComponent', () => {
  const data: UpdateToolDialogData = {
    tool: {
      id: 'tool-1',
      name: 'Lasercutter',
      description: 'Schnittbreite 40 cm',
      area: ToolArea.FabLab,
      certificationRequirement: CertificationRequirement.Yellow,
    },
    activeCertificationsCount: 24,
    existingTools: signal([
      { id: 'tool-1', name: 'Lasercutter' },
      { id: 'tool-2', name: 'Bandsäge' },
    ]),
  };

  let fixture: ComponentFixture<UpdateToolFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [UpdateToolFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UpdateToolFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  async function setName(name: string): Promise<void> {
    await (await loader.getHarness(MatInputHarness.with({ selector: '#name' }))).setValue(name);
    await fixture.whenStable();
  }

  async function chooseRequirement(label: RegExp): Promise<void> {
    const group = await loader.getHarness(MatRadioGroupHarness.with({ selector: '.requirement-options' }));
    await group.checkRadioButton({ label });
    await fixture.whenStable();
  }

  async function save(): Promise<void> {
    query('form')?.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
  }

  it('should fill the form with the tool and name it in the header', async () => {
    expect(await (await loader.getHarness(MatInputHarness.with({ selector: '#name' }))).getValue()).toBe('Lasercutter');
    expect(await (await loader.getHarness(MatInputHarness.with({ selector: '#description' }))).getValue()).toBe(
      'Schnittbreite 40 cm',
    );
    expect(query('schbar-form-dialog-header .subtitle')?.textContent?.trim()).toBe('tools.dialogs.editTool.subtitle');
    expect(query('button[type="submit"]')?.textContent?.trim()).toBe('shared.actions.saveChanges');
    expect(query('#description')?.getAttribute('placeholder')).toBe('tools.dialogs.addTool.descriptionPlaceholder');
  });

  it('should close with the update command', async () => {
    await setName('Laserschneider');

    await save();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
      toolId: 'tool-1',
      name: 'Laserschneider',
      description: 'Schnittbreite 40 cm',
      area: ToolArea.FabLab,
      certificationRequirement: CertificationRequirement.Yellow,
    });
  });

  it('should keep its own name, but reject the name of another tool', async () => {
    await setName('BANDSÄGE');

    await save();

    const field = await loader.getHarness(MatFormFieldHarness.with({ selector: ':has(#name)' }));
    expect(await field.getTextErrors()).toEqual(['tools.dialogs.addTool.errors.nameExists']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should explain the impact only when the requirement is changed', async () => {
    expect(query('.requirement-impact')).toBeNull();

    await chooseRequirement(/shared\.certificationRequirements\.RED/);

    expect(query('.requirement-impact span')?.textContent?.trim()).toBe('tools.dialogs.editTool.impactToRed');
  });
});
