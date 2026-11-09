import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDatepickerInputHarness } from '@angular/material/datepicker/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { provideDateAdapter } from '../../../shared/date/provide-date-adapter';
import { ExistingUserViewModel, NewUserDialogData, NewUserFormComponent } from './new-user-form.component';

describe('NewUserFormComponent', () => {
  const existingUsers: ExistingUserViewModel[] = [
    { id: 'user-1', firstName: 'Erika', lastName: 'Musterfrau', email: 'erika@example.org' },
  ];

  let fixture: ComponentFixture<NewUserFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };
    const data: NewUserDialogData = { existingUsers: signal(existingUsers) };

    await TestBed.configureTestingModule({
      imports: [NewUserFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        provideDateAdapter(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NewUserFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  async function setValue(id: string, value: string): Promise<void> {
    const input = await loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
    await input.setValue(value);
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

  async function click(text: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ text }));
    await button.click();
    await fixture.whenStable();
  }

  function text(selector: string): string | null {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim() ?? null;
  }

  async function fillRequiredFields(): Promise<void> {
    await setValue('firstName', 'Max');
    await setValue('lastName', 'Mustermann');
    await (await loader.getHarness(MatDatepickerInputHarness)).setValue('31.01.1990');
    await setValue('email', 'max@example.com');
    await setValue('addressLine1', 'Hauptstraße 1');
    await setValue('postalCode', '12345');
    await setValue('city', 'Berlin');
  }

  it('should preset the country', async () => {
    const country = await loader.getHarness(MatInputHarness.with({ selector: '#country' }));
    expect(await country.getValue()).toBe('Deutschland');
  });

  it('should show required errors and how many fields to check when saving an empty form', async () => {
    expect(text('.summary')).toBeNull();

    await click('users.dialogs.addNewUser.confirm');

    for (const id of ['firstName', 'lastName', 'dateOfBirth', 'email', 'addressLine1', 'postalCode', 'city']) {
      expect(await errors(id)).toEqual(['shared.errors.required']);
    }
    expect(text('.summary')).toBe('error users.dialogs.addNewUser.checkFields');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should count down the fields to check while they are filled', async () => {
    await click('users.dialogs.addNewUser.confirm');
    await fillRequiredFields();
    await setValue('city', '');

    expect(text('.summary')).toBe('error users.dialogs.addNewUser.checkOneField');

    await setValue('city', 'Berlin');

    expect(text('.summary')).toBeNull();
  });

  it('should validate email and postal code', async () => {
    await fillRequiredFields();
    await setValue('email', 'max-example.com');
    await setValue('postalCode', '123');

    await click('users.dialogs.addNewUser.confirm');

    expect(await errors('email')).toEqual(['shared.errors.email']);
    expect(await errors('postalCode')).toEqual(['shared.errors.minLength']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should point to a user with the same name or e-mail', async () => {
    expect(text('.duplicate')).toBeNull();

    await setValue('firstName', ' erika');
    await setValue('lastName', 'MUSTERFRAU ');
    expect(text('.duplicate')).toBe('warning users.dialogs.addNewUser.duplicate users.dialogs.addNewUser.openProfile');
    expect(fixture.nativeElement.querySelector('.duplicate a').getAttribute('href')).toBe('/users/user-1');

    await setValue('firstName', 'Max');
    expect(text('.duplicate')).toBeNull();

    await setValue('email', 'Erika@example.org');
    expect(text('.duplicate')).not.toBeNull();
  });

  it('should close with the create command, without empty optional fields', async () => {
    await fillRequiredFields();
    await (await loader.getHarness(MatCheckboxHarness)).check();

    await click('users.dialogs.addNewUser.confirm');

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
      firstName: 'Max',
      lastName: 'Mustermann',
      dateOfBirth: new Date(1990, 0, 31),
      clubMember: true,
      email: 'max@example.com',
      phone: undefined,
      addressLine1: 'Hauptstraße 1',
      addressLine2: undefined,
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    });
  });

  it('should only close the dialog on cancel without saving', async () => {
    await click('shared.actions.cancel');

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
