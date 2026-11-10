import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { UserOpenSessionComponent, UserOpenSessionViewModel } from './user-open-session.component';

registerLocaleData(localeDe);

describe('UserOpenSessionComponent', () => {
  const at = (hours: number, minutes: number) => new Date(2026, 10, 9, hours, minutes);
  const session: UserOpenSessionViewModel = {
    usages: [
      {
        id: 'w-1',
        kind: 'workshop',
        toolName: null,
        startTime: at(14, 5),
        endTime: null,
        durationInMinutes: null,
        units: null,
      },
      {
        id: 't-1',
        kind: 'tool',
        toolName: 'Bandsäge',
        startTime: at(14, 12),
        endTime: at(14, 31),
        durationInMinutes: 19,
        units: 1,
      },
    ],
    workshopMinutes: 83,
    workshopUnits: 2,
    toolUnits: 3,
  };

  let fixture: ComponentFixture<UserOpenSessionComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [UserOpenSessionComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        // times like in the app: 14:05, not 2:05 PM
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    });
    fixture = TestBed.createComponent(UserOpenSessionComponent);
    fixture.componentRef.setInput('userOpenSession', session);
    await fixture.whenStable();
  });

  function rows(): string[][] {
    return [...fixture.nativeElement.querySelectorAll('tr.mat-mdc-row')].map((row: HTMLElement) =>
      [...row.querySelectorAll('td')].map((cell) => cell.textContent!.trim()),
    );
  }

  it('should list the usages with "running" and dashes for values that do not exist yet', () => {
    expect(rows()).toEqual([
      ['users.pages.openSession.workshop', '14:05', 'users.pages.openSession.running', '–', '–'],
      ['Bandsäge', '14:12', '14:31', 'users.pages.openSession.minutes', '1'],
    ]);
  });

  it('should show the totals', () => {
    const text = (selector: string) => fixture.nativeElement.querySelector(selector).textContent.trim();

    expect(text('.workshop-minutes')).toBe('users.pages.openSession.minutes');
    expect(text('.workshop-units')).toBe('2');
    expect(text('.tool-units')).toBe('3');
  });
});
