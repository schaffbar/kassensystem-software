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
import { User } from '../../shared/models/user.model';
import { UpdateUserFormComponent } from './update-user-form.component';

describe('UpdateUserFormComponent', () => {
  // the backend sends the date as ISO string
  const user = {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: '1990-01-31' as unknown as Date,
    clubMember: false,
  } as User;

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

  async function click(text: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ text }));
    await button.click();
    await fixture.whenStable();
  }

  it('should prefill the form with the user', async () => {
    expect(await (await input('firstName')).getValue()).toBe('Max');
    expect(await (await input('lastName')).getValue()).toBe('Mustermann');
    expect(await (await loader.getHarness(MatDatepickerInputHarness)).getValue()).toBe('31.01.1990');
    expect(await (await loader.getHarness(MatCheckboxHarness)).isChecked()).toBe(false);
  });

  async function dateOfBirthErrors(): Promise<string[]> {
    const field = await loader.getHarness(
      MatFormFieldHarness.with({ floatingLabelText: 'users.shared.userProperties.dateOfBirth' }),
    );
    return field.getTextErrors();
  }

  it('should accept a typed German date', async () => {
    await (await loader.getHarness(MatDatepickerInputHarness)).setValue('15.3.1985');

    await click('users.dialogs.editUser.actions.confirm');

    expect(dialogRef.close).toHaveBeenCalledOnce();
    expect(dialogRef.close.mock.calls[0][0].dateOfBirth).toEqual(new Date(1985, 2, 15));
  });

  it('should reject a date of birth outside the allowed range (e.g. two-digit year)', async () => {
    await (await loader.getHarness(MatDatepickerInputHarness)).setValue('31.01.90');

    await click('users.dialogs.editUser.actions.confirm');

    expect(await dateOfBirthErrors()).toEqual(['shared.errors.invalid']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should require a date of birth', async () => {
    await (await loader.getHarness(MatDatepickerInputHarness)).setValue('');

    await click('users.dialogs.editUser.actions.confirm');

    expect(await dateOfBirthErrors()).toEqual(['shared.errors.required']);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should close with the update command', async () => {
    await (await input('firstName')).setValue('Moritz');
    await (await loader.getHarness(MatCheckboxHarness)).check();

    await click('users.dialogs.editUser.actions.confirm');

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
      id: 'user-1',
      firstName: 'Moritz',
      lastName: 'Mustermann',
      dateOfBirth: new Date(1990, 0, 31),
      clubMember: true,
    });
  });

  it('should require first and last name', async () => {
    await (await input('firstName')).setValue('');
    await (await input('lastName')).setValue('');

    await click('users.dialogs.editUser.actions.confirm');

    for (const label of ['firstName', 'lastName']) {
      const field = await loader.getHarness(
        MatFormFieldHarness.with({ floatingLabelText: `users.shared.userProperties.${label}` }),
      );
      expect(await field.getTextErrors()).toEqual(['shared.errors.required']);
    }
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should only close the dialog on cancel without saving', async () => {
    await click('users.dialogs.editUser.actions.cancel');

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
