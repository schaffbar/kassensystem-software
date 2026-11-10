import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDatepickerInputHarness } from '@angular/material/datepicker/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { provideDateAdapter } from '../../../shared/date/provide-date-adapter';
import { UpdateUserFormComponent, UpdateUserViewModel } from './update-user-form.component';

describe('UpdateUserFormComponent', () => {
  const user: UpdateUserViewModel = {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1990, 0, 31),
    clubMember: false,
    email: 'max@example.org',
    phone: null,
    address: {
      addressLine1: 'Hauptstraße 1',
      addressLine2: null,
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    },
    createdAt: new Date(2026, 0, 1),
  };

  let fixture: ComponentFixture<UpdateUserFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [UpdateUserFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        provideDateAdapter(),
        { provide: MAT_DIALOG_DATA, useValue: { user } },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UpdateUserFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function input(id: string): Promise<MatInputHarness> {
    return loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
  }

  async function setValue(id: string, value: string): Promise<void> {
    await (await input(id)).setValue(value);
    await fixture.whenStable();
  }

  // the label is above the field, so the field is found by its input
  async function errors(id: string): Promise<string[]> {
    const fields = await loader.getAllHarnesses(MatFormFieldHarness);
    for (const field of fields) {
      const control = await field.getControl();
      if (control && (await (await control.host()).getAttribute('id')) === id) return field.getTextErrors();
    }
    throw new Error(`no field #${id}`);
  }

  function saveButton(): Promise<MatButtonHarness> {
    return loader.getHarness(MatButtonHarness.with({ text: 'shared.actions.saveChanges' }));
  }

  async function save(): Promise<void> {
    await (await saveButton()).click();
    await fixture.whenStable();
  }

  function text(selector: string): string | null {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim() ?? null;
  }

  it('should prefill person, contact and address of the user', async () => {
    expect(await (await input('firstName')).getValue()).toBe('Max');
    expect(await (await input('lastName')).getValue()).toBe('Mustermann');
    expect(await (await loader.getHarness(MatDatepickerInputHarness)).getValue()).toBe('31.01.1990');
    expect(await (await loader.getHarness(MatCheckboxHarness)).isChecked()).toBe(false);
    expect(await (await input('email')).getValue()).toBe('max@example.org');
    expect(await (await input('phone')).getValue()).toBe('');
    expect(await (await input('addressLine1')).getValue()).toBe('Hauptstraße 1');
    expect(await (await input('addressLine2')).getValue()).toBe('');
    expect(await (await input('postalCode')).getValue()).toBe('12345');
    expect(await (await input('city')).getValue()).toBe('Berlin');
    expect(await (await input('country')).getValue()).toBe('Deutschland');
  });

  it('should count the unsaved changes and only save with a change', async () => {
    expect(text('.changes')).toBe('users.dialogs.editUser.noChanges');
    expect(await (await saveButton()).isDisabled()).toBe(true);

    await setValue('firstName', 'Moritz');
    expect(text('.changes')).toBe('users.dialogs.editUser.oneChange');

    await setValue('city', 'Potsdam');
    expect(text('.changes')).toBe('users.dialogs.editUser.changes');
    expect(await (await saveButton()).isDisabled()).toBe(false);

    // back to the saved value: no change any more
    await setValue('firstName', 'Max');
    await setValue('city', 'Berlin');
    expect(text('.changes')).toBe('users.dialogs.editUser.noChanges');
  });

  it('should close with all fields and missing optional values as undefined', async () => {
    await setValue('firstName', 'Moritz');
    await (await loader.getHarness(MatCheckboxHarness)).check();
    await setValue('addressLine1', 'Hauptstraße 2');

    await save();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
      userId: 'user-1',
      firstName: 'Moritz',
      lastName: 'Mustermann',
      dateOfBirth: new Date(1990, 0, 31),
      clubMember: true,
      email: 'max@example.org',
      phone: undefined,
      addressLine1: 'Hauptstraße 2',
      addressLine2: undefined,
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    });
  });

  it('should accept a typed German date', async () => {
    await (await loader.getHarness(MatDatepickerInputHarness)).setValue('15.3.1985');

    await save();

    expect(dialogRef.close.mock.calls[0][0].dateOfBirth).toEqual(new Date(1985, 2, 15));
  });

  it('should reject a date of birth outside the allowed range (e.g. two-digit year)', async () => {
    await (await loader.getHarness(MatDatepickerInputHarness)).setValue('31.01.90');

    await save();

    expect(await errors('dateOfBirth')).toEqual(['shared.errors.invalid']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should check required fields, email and postal code and say how many fields need attention', async () => {
    await setValue('firstName', '');
    await setValue('email', 'max@');
    await setValue('postalCode', '123');

    await save();

    expect(await errors('firstName')).toEqual(['shared.errors.required']);
    expect(await errors('email')).toEqual(['shared.errors.email']);
    expect(await errors('postalCode')).toEqual(['shared.errors.minLength']);
    expect(text('.summary')).toBe('error users.dialogs.userForm.checkFields');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should only close the dialog on cancel without saving', async () => {
    await (await loader.getHarness(MatButtonHarness.with({ text: 'shared.actions.cancel' }))).click();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
