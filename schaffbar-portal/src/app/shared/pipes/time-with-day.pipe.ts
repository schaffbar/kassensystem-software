import { formatDate } from '@angular/common';
import { inject, LOCALE_ID, Pipe, PipeTransform } from '@angular/core';

import { isSameDay } from 'date-fns';

/**
 * A time of today as time only (`17:50`), of another day with the date (`10.11.26, 17:50`) – a session that began
 * yesterday must not look like one of today.
 *
 * `{{ session.startTime | timeWithDay }}`
 */
@Pipe({ name: 'timeWithDay' })
export class TimeWithDayPipe implements PipeTransform {
  private readonly locale = inject(LOCALE_ID);

  transform(value: Date, today: Date = new Date()): string {
    return formatDate(value, isSameDay(value, today) ? 'shortTime' : 'short', this.locale);
  }
}
