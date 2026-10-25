import { Provider } from '@angular/core';
import { provideDateFnsAdapter } from '@angular/material-date-fns-adapter';

import { MAT_DATE_LOCALE } from '@angular/material/core';

import { de } from 'date-fns/locale';

/**
 * Material datepicker based on date-fns with the German locale: shows dates as dd.MM.yyyy and parses typed dates
 * like "31.01.1990", "31.1.1990" or "1.2.1990". Used by the app config and by tests of components with a datepicker.
 */
export function provideDateAdapter(): Provider[] {
  return [provideDateFnsAdapter(), { provide: MAT_DATE_LOCALE, useValue: de }];
}
