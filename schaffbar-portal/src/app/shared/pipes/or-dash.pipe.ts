import { Pipe, PipeTransform } from '@angular/core';

/**
 * Shows a dash for a missing value (`null`, `undefined`, `''`) – only for display (tables, `readonly-field`),
 * never for the value of an editable form field.
 *
 * `{{ usage.unitsUsed | orDash }}`, `{{ usage.endTime | date: 'medium' | orDash }}`
 */
@Pipe({ name: 'orDash' })
export class OrDashPipe implements PipeTransform {
  transform<T>(value: T | null | undefined): T | string {
    return value === null || value === undefined || value === '' ? '–' : value;
  }
}
