import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { TimeWithDayPipe } from './time-with-day.pipe';

registerLocaleData(localeDe);

describe('TimeWithDayPipe', () => {
  const today = new Date(2026, 10, 11, 9, 0);
  let pipe: TimeWithDayPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [{ provide: LOCALE_ID, useValue: 'de-DE' }] });
    pipe = TestBed.runInInjectionContext(() => new TimeWithDayPipe());
  });

  it('should show a time of today without date', () => {
    expect(pipe.transform(new Date(2026, 10, 11, 17, 50), today)).toBe('17:50');
  });

  it('should add the date to a time of another day', () => {
    expect(pipe.transform(new Date(2026, 10, 10, 17, 50), today)).toBe('10.11.26, 17:50');
  });
});
