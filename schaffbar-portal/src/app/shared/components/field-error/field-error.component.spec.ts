import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { email, form, minLength, required, SchemaFn, validate } from '@angular/forms/signals';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { FieldErrorComponent } from './field-error.component';

interface TestModel {
  name: string;
}

describe('FieldErrorComponent', () => {
  let fixture: ComponentFixture<FieldErrorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FieldErrorComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('de', {
      shared: {
        errors: {
          required: '{{field}} ist erforderlich',
          email: 'Bitte geben Sie eine gültige E-Mail-Adresse ein',
          minLength: '{{field}} muss mindestens {{value}} Zeichen lang sein',
        },
      },
      labels: { name: 'Name' },
      custom: { nameExists: 'Name existiert bereits' },
    });
    translate.use('de');

    fixture = TestBed.createComponent(FieldErrorComponent);
  });

  async function render(value: string, schema: SchemaFn<TestModel>): Promise<string> {
    const testForm = TestBed.runInInjectionContext(() => form(signal<TestModel>({ name: value }), schema));
    fixture.componentRef.setInput('field', testForm.name);
    fixture.componentRef.setInput('label', 'labels.name');
    await fixture.whenStable();
    return fixture.nativeElement.textContent.trim();
  }

  it('should render nothing when the field is valid', async () => {
    expect(await render('Max', (schema) => required(schema.name))).toBe('');
  });

  it('should translate required with the field label', async () => {
    expect(await render('', (schema) => required(schema.name))).toBe('Name ist erforderlich');
  });

  it('should translate email', async () => {
    expect(await render('no-mail', (schema) => email(schema.name))).toBe(
      'Bitte geben Sie eine gültige E-Mail-Adresse ein',
    );
  });

  it('should pass the length of minLength as value', async () => {
    expect(await render('abc', (schema) => minLength(schema.name, 5))).toBe('Name muss mindestens 5 Zeichen lang sein');
  });

  it('should use the message of a custom error as translation key', async () => {
    const schema: SchemaFn<TestModel> = (path) =>
      validate(path.name, () => ({ kind: 'nameExists', message: 'custom.nameExists' }));

    expect(await render('Max', schema)).toBe('Name existiert bereits');
  });

  it('should only show the first error', async () => {
    const schema: SchemaFn<TestModel> = (path) => {
      required(path.name);
      validate(path.name, () => ({ kind: 'nameExists', message: 'custom.nameExists' }));
    };

    expect(await render('', schema)).toBe('Name ist erforderlich');
  });
});
