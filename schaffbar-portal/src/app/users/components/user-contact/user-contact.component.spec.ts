import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { User } from '../../shared/models/user.model';
import { UserContactComponent } from './user-contact.component';

describe('UserContactComponent', () => {
  const user = { id: 'user-1', email: 'max@example.com', phone: '0123 456' } as User;

  let fixture: ComponentFixture<UserContactComponent>;
  let loader: HarnessLoader;
  let contactChanged: Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserContactComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(UserContactComponent);
    fixture.componentRef.setInput('user', user);
    contactChanged = vi.fn();
    fixture.componentInstance.contactChanged.subscribe(contactChanged);
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

  async function emailErrors(): Promise<string[]> {
    const field = await loader.getHarness(
      MatFormFieldHarness.with({ floatingLabelText: 'users.shared.userProperties.email' }),
    );
    return field.getTextErrors();
  }

  const editButton = '[aria-label="Edit contact"]';
  const cancelButton = '.form-actions button[type="button"]';
  const saveButton = '.form-actions button[type="submit"]';

  it('should show the contact read-only', async () => {
    expect(await (await input('email')).getValue()).toBe('max@example.com');
    expect(await (await input('email')).isReadonly()).toBe(true);
    expect(await (await input('phone')).isReadonly()).toBe(true);
  });

  it('should make the fields editable and focus the email after clicking edit', async () => {
    await click(editButton);

    expect(await (await input('email')).isReadonly()).toBe(false);
    expect(await (await input('email')).isFocused()).toBe(true);
    expect(await (await input('phone')).isReadonly()).toBe(false);
  });

  it('should emit the contact on save and switch back to read-only', async () => {
    await click(editButton);
    await (await input('phone')).setValue('0987 654');

    await click(saveButton);

    expect(contactChanged).toHaveBeenCalledExactlyOnceWith({
      id: 'user-1',
      email: 'max@example.com',
      phone: '0987 654',
    });
    expect(await (await input('email')).isReadonly()).toBe(true);
  });

  it('should require an email address', async () => {
    await click(editButton);
    await (await input('email')).setValue('');

    await click(saveButton);

    expect(await emailErrors()).toEqual(['shared.errors.required']);
    expect(contactChanged).not.toHaveBeenCalled();
  });

  it('should reject an invalid email address', async () => {
    await click(editButton);
    await (await input('email')).setValue('max-example.com');

    await click(saveButton);

    expect(await emailErrors()).toEqual(['shared.errors.email']);
    expect(contactChanged).not.toHaveBeenCalled();
  });

  it('should restore the values on cancel without emitting', async () => {
    await click(editButton);
    await (await input('email')).setValue('other@example.com');

    await click(cancelButton);

    expect(contactChanged).not.toHaveBeenCalled();
    expect(await (await input('email')).getValue()).toBe('max@example.com');
  });
});
