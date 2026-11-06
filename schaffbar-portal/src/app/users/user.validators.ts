import { maxLength, minLength, required, SchemaPath, validate } from '@angular/forms/signals';

import { endOfYear, startOfYear, subYears } from 'date-fns';

export interface DateRange {
  min: Date;
  max: Date;
}

/**
 * A customer is at most 100 years old and was born before the current year. A function, not a constant – the range
 * moves with the year, also in a tab that stays open over New Year.
 */
export function dateOfBirthRange(today = new Date()): DateRange {
  return { min: startOfYear(subYears(today, 100)), max: endOfYear(subYears(today, 1)) };
}

export const POSTAL_CODE_LENGTH = 5;

/**
 * Required date of birth within {@link dateOfBirthRange}.
 * The datepicker restricts the selection to that range, this also covers typed dates (e.g. "31.01.90" = year 90).
 */
export function validateDateOfBirth(path: SchemaPath<Date | null>): void {
  required(path);
  validate(path, ({ value }) => {
    const date = value();
    if (!date) return undefined;
    const { min, max } = dateOfBirthRange();
    return date < min || date > max ? { kind: 'dateOfBirthOutOfRange' } : undefined;
  });
}

/** Required German postal code with exactly 5 characters ([formField] also sets the native maxlength). */
export function validatePostalCode(path: SchemaPath<string>): void {
  required(path);
  minLength(path, POSTAL_CODE_LENGTH);
  maxLength(path, POSTAL_CODE_LENGTH);
}
