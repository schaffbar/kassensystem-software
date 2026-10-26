import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { User, UserAddress } from '../../shared/models/user.model';
import { UserAddressComponent } from './user-address.component';

describe('UserAddressComponent', () => {
  const address: UserAddress = {
    addressLine1: 'Hauptstraße 1',
    addressLine2: '',
    postalCode: '12345',
    city: 'Berlin',
    country: 'Deutschland',
  };

  let fixture: ComponentFixture<UserAddressComponent>;
  let loader: HarnessLoader;
  let addressChanged: Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserAddressComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(UserAddressComponent);
    fixture.componentRef.setInput('user', { id: 'user-1', address } as User);
    addressChanged = vi.fn();
    fixture.componentInstance.addressChanged.subscribe(addressChanged);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function input(id: string): Promise<MatInputHarness> {
    return loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
  }

  async function click(selector: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ selector }));
    await button.click();
    await fixture.whenStable();
  }

  async function errors(label: string): Promise<string[]> {
    const field = await loader.getHarness(
      MatFormFieldHarness.with({ floatingLabelText: `users.shared.userProperties.${label}` }),
    );
    return field.getTextErrors();
  }

  const editButton = '[aria-label="Edit address"]';
  const cancelButton = '.form-actions button[type="button"]';
  const saveButton = '.form-actions button[type="submit"]';
  const fields = ['addressLine1', 'addressLine2', 'postalCode', 'city', 'country'];

  it('should show all address fields read-only', async () => {
    for (const id of fields) {
      expect(await (await input(id)).isReadonly()).toBe(true);
    }
  });

  it('should make all fields editable and focus the first one after clicking edit', async () => {
    await click(editButton);

    for (const id of fields) {
      expect(await (await input(id)).isReadonly()).toBe(false);
    }
    expect(await (await input('addressLine1')).isFocused()).toBe(true);
  });

  it('should emit the address on save and switch back to read-only', async () => {
    await click(editButton);
    await (await input('city')).setValue('Potsdam');
    await (await input('postalCode')).setValue('14467');

    await click(saveButton);

    expect(addressChanged).toHaveBeenCalledExactlyOnceWith({
      id: 'user-1',
      ...address,
      city: 'Potsdam',
      postalCode: '14467',
    });
    expect(await (await input('city')).isReadonly()).toBe(true);
  });

  it('should require address line 1, postal code, city and country', async () => {
    await click(editButton);
    for (const id of ['addressLine1', 'postalCode', 'city', 'country']) {
      await (await input(id)).setValue('');
    }

    await click(saveButton);

    for (const label of ['addressLine1', 'postalCode', 'city', 'country']) {
      expect(await errors(label)).toEqual(['shared.errors.required']);
    }
    expect(addressChanged).not.toHaveBeenCalled();
  });

  it('should require exactly 5 characters for the postal code', async () => {
    const postalCode = fixture.nativeElement.querySelector('#postalCode') as HTMLInputElement;
    expect(postalCode.maxLength).toBe(5);

    await click(editButton);
    await (await input('postalCode')).setValue('123');
    await click(saveButton);

    expect(await errors('postalCode')).toEqual(['shared.errors.minLength']);
    expect(addressChanged).not.toHaveBeenCalled();
  });

  it('should show the postal code length as hint', async () => {
    await click(editButton);
    await (await input('postalCode')).setValue('123');

    const field = await loader.getHarness(
      MatFormFieldHarness.with({ floatingLabelText: 'users.shared.userProperties.postalCode' }),
    );
    expect(await field.getTextHints()).toEqual(['3 / 5']);
  });

  it('should restore the values on cancel without emitting', async () => {
    await click(editButton);
    await (await input('city')).setValue('Potsdam');

    await click(cancelButton);

    expect(addressChanged).not.toHaveBeenCalled();
    expect(await (await input('city')).getValue()).toBe('Berlin');
  });
});
