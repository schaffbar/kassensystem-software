import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDatepickerInputHarness } from '@angular/material/datepicker/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { provideDateAdapter } from '../../../shared/date/provide-date-adapter';
import { NewUserFormComponent } from './new-user-form.component';

describe('NewUserFormComponent', () => {
  let fixture: ComponentFixture<NewUserFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [NewUserFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        provideDateAdapter(),
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NewUserFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  async function setValue(id: string, value: string): Promise<void> {
    const input = await loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
    await input.setValue(value);
  }

  async function errors(label: string): Promise<string[]> {
    const field = await loader.getHarness(
      MatFormFieldHarness.with({ floatingLabelText: `users.shared.userProperties.${label}` }),
    );
    return field.getTextErrors();
  }

  async function click(text: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ text }));
    await button.click();
    await fixture.whenStable();
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

  it('should show required errors and not close when saving an empty form', async () => {
    await click('users.dialogs.addNewUser.actions.confirm');

    for (const label of ['firstName', 'lastName', 'dateOfBirth', 'email', 'addressLine1', 'postalCode', 'city']) {
      expect(await errors(label)).toEqual(['shared.errors.required']);
    }
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should validate email and postal code', async () => {
    await fillRequiredFields();
    await setValue('email', 'max-example.com');
    await setValue('postalCode', '123');

    await click('users.dialogs.addNewUser.actions.confirm');

    expect(await errors('email')).toEqual(['shared.errors.email']);
    expect(await errors('postalCode')).toEqual(['shared.errors.minLength']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should close with the create command, without empty optional fields', async () => {
    await fillRequiredFields();
    await (await loader.getHarness(MatCheckboxHarness)).check();

    await click('users.dialogs.addNewUser.actions.confirm');

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
    await click('users.dialogs.addNewUser.actions.cancel');

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
