import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import {
  CreateToolCertificationsDialogData,
  CreateToolCertificationsFormComponent,
} from './create-tool-certifications-form.component';

describe('CreateToolCertificationsFormComponent', () => {
  const lena = { id: 'user-1', firstName: 'Lena', lastName: 'Hoffmann', email: 'lena@example.org' };
  const paul = { id: 'user-2', firstName: 'Paul', lastName: 'Richter', email: 'paul@example.org' };
  const data: CreateToolCertificationsDialogData = {
    toolId: 'tool-1',
    toolName: 'Lasercutter',
    certifiers: [paul, lena],
    candidates: [
      { id: 'user-3', firstName: 'Sophie', lastName: 'Wagner', email: 'sophie.wagner@example.org' },
      { id: 'user-4', firstName: 'Tim', lastName: 'Schröder', email: 'tim@example.org' },
    ],
  };

  let fixture: ComponentFixture<CreateToolCertificationsFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };

  async function setUp(dialogData: CreateToolCertificationsDialogData): Promise<void> {
    dialogRef = { close: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [CreateToolCertificationsFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateToolCertificationsFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  }

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  }

  function saveButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('mat-dialog-actions button[matButton="filled"]');
  }

  async function select(name: string): Promise<void> {
    await (await loader.getHarness(MatCheckboxHarness.with({ label: new RegExp(name) }))).check();
    await fixture.whenStable();
  }

  async function chooseCertifier(name: string): Promise<void> {
    const certifier = await loader.getHarness(MatSelectHarness);
    await certifier.open();
    await certifier.clickOptions({ text: name });
    await fixture.whenStable();
  }

  describe('with instructors', () => {
    beforeEach(() => setUp(data));

    it('should offer the instructors and the people without certification, sorted by name', async () => {
      const certifier = await loader.getHarness(MatSelectHarness);
      await certifier.open();
      expect(await Promise.all((await certifier.getOptions()).map((option) => option.getText()))).toEqual([
        'Lena Hoffmann',
        'Paul Richter',
      ]);
      expect(texts('.candidate-name')).toEqual(['Sophie Wagner', 'Tim Schröder']);
    });

    it('should search by name and e-mail address', async () => {
      const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
      input.value = 'tim@';
      input.dispatchEvent(new Event('input'));
      await fixture.whenStable();

      expect(texts('.candidate-name')).toEqual(['Tim Schröder']);
    });

    it('should show the picked people above the list and remove them there', async () => {
      await select('Tim');
      await select('Sophie');
      expect(texts('.selected-person-name')).toEqual(['Tim Schröder', 'Sophie Wagner']);

      fixture.nativeElement.querySelector('.selected-person button').click();
      await fixture.whenStable();

      expect(texts('.selected-person-name')).toEqual(['Sophie Wagner']);
    });

    it('should save only with an instructor and at least one person', async () => {
      expect(saveButton().disabled).toBe(true);

      await select('Sophie');
      expect(saveButton().disabled).toBe(true);

      await chooseCertifier('Lena Hoffmann');
      expect(saveButton().disabled).toBe(false);
      expect(saveButton().textContent?.trim()).toBe('tools.dialogs.createCertifications.confirmOne');

      saveButton().click();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        userIds: ['user-3'],
        toolId: 'tool-1',
        certifiedBy: 'user-1',
      });
    });
  });

  it('should preselect the only instructor', async () => {
    await setUp({ ...data, certifiers: [lena] });
    await select('Sophie');

    saveButton().click();

    expect(dialogRef.close).toHaveBeenCalledWith(expect.objectContaining({ certifiedBy: 'user-1' }));
  });

  it('should explain that an instructor is needed first', async () => {
    await setUp({ ...data, certifiers: [] });

    expect(texts('.no-certifiers')).toEqual(['info tools.dialogs.createCertifications.noCertifiers']);
    expect(fixture.nativeElement.querySelector('mat-select')).toBeNull();
  });
});
