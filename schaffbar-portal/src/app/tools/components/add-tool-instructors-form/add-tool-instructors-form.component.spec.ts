import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { AddToolInstructorsDialogData, AddToolInstructorsFormComponent } from './add-tool-instructors-form.component';

describe('AddToolInstructorsFormComponent', () => {
  const data: AddToolInstructorsDialogData = {
    toolId: 'tool-1',
    toolName: 'Lasercutter',
    candidates: [
      {
        id: 'user-1',
        firstName: 'Mia',
        lastName: 'Fischer',
        certification: { status: ToolCertificationStatus.Active, certifiedAt: new Date('2025-09-04T10:00:00Z') },
      },
      {
        id: 'user-2',
        firstName: 'Anna',
        lastName: 'Becker',
        certification: { status: ToolCertificationStatus.Paused, certifiedAt: new Date('2025-01-01T10:00:00Z') },
      },
      { id: 'user-3', firstName: 'Felix', lastName: 'Lang', certification: null },
      {
        id: 'user-4',
        firstName: 'Jonas',
        lastName: 'Weber',
        certification: { status: ToolCertificationStatus.Active, certifiedAt: new Date('2026-02-27T10:00:00Z') },
      },
    ],
  };

  let fixture: ComponentFixture<AddToolInstructorsFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [AddToolInstructorsFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddToolInstructorsFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  }

  function saveButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('mat-dialog-actions button[matButton="filled"]');
  }

  async function select(name: string): Promise<void> {
    const checkbox = await loader.getHarness(
      MatCheckboxHarness.with({ selector: '.candidate', label: new RegExp(name) }),
    );
    await checkbox.check();
    await fixture.whenStable();
  }

  it('should show only the people with an active certification, sorted by name', () => {
    expect(texts('.candidate-name')).toEqual(['Jonas Weber', 'Mia Fischer']);
    expect(texts('.candidate-meta')).toEqual([
      'tools.dialogs.addInstructors.certifiedSince',
      'tools.dialogs.addInstructors.certifiedSince',
    ]);
  });

  it('should show everybody when the filter is switched off', async () => {
    await (await loader.getHarness(MatCheckboxHarness.with({ selector: '.only-certified' }))).uncheck();
    await fixture.whenStable();

    expect(texts('.candidate-name')).toEqual(['Anna Becker', 'Felix Lang', 'Jonas Weber', 'Mia Fischer']);
    expect(texts('.candidate-meta').slice(0, 2)).toEqual([
      'tools.dialogs.addInstructors.paused',
      'tools.dialogs.addInstructors.notCertified',
    ]);
  });

  it('should search by name', async () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = 'mia';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(texts('.candidate-name')).toEqual(['Mia Fischer']);
  });

  it('should clear the search', async () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    expect(fixture.nativeElement.querySelector('.search-field button')).toBeNull();
    input.value = 'mia';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    fixture.nativeElement.querySelector('.search-field button').click();
    await fixture.whenStable();

    expect(input.value).toBe('');
    expect(texts('.candidate-name')).toEqual(['Jonas Weber', 'Mia Fischer']);
  });

  it('should add nothing before a person is selected', () => {
    expect(saveButton().disabled).toBe(true);
    expect(saveButton().textContent?.trim()).toBe('tools.dialogs.addInstructors.confirm');
  });

  it('should close with the selected people, counting them in the button', async () => {
    await select('Mia Fischer');
    await select('Jonas Weber');

    expect(saveButton().textContent?.trim()).toBe('tools.dialogs.addInstructors.confirmCount');
    saveButton().click();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', instructorIds: ['user-1', 'user-4'] });
  });

  it('should keep the selection while searching', async () => {
    await select('Mia Fischer');
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = 'jonas';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    saveButton().click();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', instructorIds: ['user-1'] });
  });
});
