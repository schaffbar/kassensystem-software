import { maxLength, minLength, required, SchemaPath, validate } from '@angular/forms/signals';

import { endOfYear, startOfYear, subYears } from 'date-fns';

/** A customer is at most 100 years old and was born before the current year. */
export const DATE_OF_BIRTH_MIN = startOfYear(subYears(new Date(), 100));
export const DATE_OF_BIRTH_MAX = endOfYear(subYears(new Date(), 1));

export const POSTAL_CODE_LENGTH = 5;

/**
 * Required date of birth within {@link DATE_OF_BIRTH_MIN} and {@link DATE_OF_BIRTH_MAX}.
 * The datepicker restricts the selection to that range, this also covers typed dates (e.g. "31.01.90" = year 90).
 */
export function validateDateOfBirth(path: SchemaPath<Date | null>): void {
  required(path);
  validate(path, ({ value }) => {
    const date = value();
    if (!date) return undefined;
    return date < DATE_OF_BIRTH_MIN || date > DATE_OF_BIRTH_MAX ? { kind: 'dateOfBirthOutOfRange' } : undefined;
  });
}

/** Required German postal code with exactly 5 characters ([formField] also sets the native maxlength). */
export function validatePostalCode(path: SchemaPath<string>): void {
  required(path);
  minLength(path, POSTAL_CODE_LENGTH);
  maxLength(path, POSTAL_CODE_LENGTH);
}
