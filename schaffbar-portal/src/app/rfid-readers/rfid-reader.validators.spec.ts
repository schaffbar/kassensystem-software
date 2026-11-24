import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form, ValidationError } from '@angular/forms/signals';

import { beforeEach, describe, expect, it } from 'vitest';

import { RfidReader } from './rfid-reader.model';
import { validateUniqueRfidReaderName } from './rfid-reader.validators';

describe('validateUniqueRfidReaderName', () => {
  const rfidReaders = signal<Pick<RfidReader, 'id' | 'name'>[]>([]);

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
    rfidReaders.set([
      { id: 'reader-1', name: 'Theke' },
      { id: 'reader-2', name: null },
    ]);
  });

  function nameErrors(name: string): () => ValidationError[] {
    const nameForm = TestBed.runInInjectionContext(() =>
      form(signal({ name }), (schema) => validateUniqueRfidReaderName(schema.name, 'reader-2', rfidReaders)),
    );
    return () => nameForm.name().errors();
  }

  it('should reject the name of another reader with the text of the dialog', () => {
    expect(nameErrors('THEKE')()).toEqual([
      expect.objectContaining({
        kind: 'nameExists',
        message: 'rfidReaders.dialogs.updateRfidReader.errors.nameExists',
      }),
    ]);
  });

  it('should accept a free name and an empty field', () => {
    expect(nameErrors('Theke 2')()).toEqual([]);
    expect(nameErrors('')()).toEqual([]);
  });

  it('should follow the list of readers', () => {
    const errors = nameErrors('Theke');
    expect(errors()).toEqual([expect.objectContaining({ kind: 'nameExists' })]);

    rfidReaders.set([{ id: 'reader-1', name: 'Werkstatt Eingang' }]);

    expect(errors()).toEqual([]);
  });
});
