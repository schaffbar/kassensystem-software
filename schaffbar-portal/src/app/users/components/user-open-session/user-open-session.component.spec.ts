import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UserOpenSessionComponent, UserOpenSessionViewModel } from './user-open-session.component';

registerLocaleData(localeDe);

describe('UserOpenSessionComponent', () => {
  const at = (hours: number, minutes: number) => new Date(2026, 10, 9, hours, minutes);
  const session: UserOpenSessionViewModel = {
    stays: [
      {
        id: 'w-1',
        startTime: at(17, 49),
        endTime: at(19, 4),
        durationInMinutes: 75,
        units: 2,
        toolUsages: [
          {
            id: 'k-1',
            toolName: 'Kreissäge',
            startTime: at(17, 50),
            endTime: at(17, 54),
            durationInMinutes: 4,
            units: 1,
          },
        ],
      },
      { id: 'w-2', startTime: at(19, 49), endTime: null, durationInMinutes: null, units: null, toolUsages: [] },
    ],
    workshopMinutes: 100,
    workshopUnits: 3,
    toolUnits: 1,
    tools: [{ toolId: 'tool-1', toolName: 'Kreissäge', usages: 1, minutes: 4, units: 1 }],
  };

  let fixture: ComponentFixture<UserOpenSessionComponent>;

  beforeEach(async () => {
    // the times of the test data are of "today" – only the clock, the timers stay real
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 10, 9, 22, 0) });
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

  afterEach(() => vi.useRealTimers());

  function rows(): string[][] {
    return [...fixture.nativeElement.querySelectorAll('tr.mat-mdc-row')].map((row: HTMLElement) =>
      [...row.querySelectorAll('td')].map((cell) => cell.textContent!.trim()),
    );
  }

  it('should list each stay with the tools used during it indented below', () => {
    expect(rows()).toEqual([
      ['users.pages.openSession.workshop', '17:49', '19:04', 'users.pages.openSession.minutes', '2'],
      ['Kreissäge', '17:50', '17:54', 'users.pages.openSession.minutes', '1'],
      ['users.pages.openSession.workshop', '19:49', 'users.pages.openSession.running', '–', '–'],
    ]);
    const classes = [...fixture.nativeElement.querySelectorAll('tr.mat-mdc-row')].map((row: HTMLElement) =>
      row.classList.contains('tool-row') ? 'tool' : 'stay',
    );
    expect(classes).toEqual(['stay', 'tool', 'stay']);
  });

  it('should add the date to the times of a stay that began on an earlier day', async () => {
    fixture.componentRef.setInput('userOpenSession', {
      ...session,
      stays: [{ ...session.stays[1], startTime: new Date(2026, 10, 8, 19, 49) }],
    });
    await fixture.whenStable();

    expect(rows()[0][1]).toBe('08.11.26, 19:49');
  });

  it('should show the totals', () => {
    const text = (selector: string) => fixture.nativeElement.querySelector(selector).textContent.trim();

    expect(text('.workshop-minutes')).toBe('users.pages.openSession.minutes');
    expect(text('.workshop-units')).toBe('3');
    expect(text('.tool-units')).toBe('1');
  });

  it('should show the totals of each tool', () => {
    const lines = [...fixture.nativeElement.querySelectorAll('.tool-totals div')].map((line: HTMLElement) =>
      line.textContent!.replace(/\s+/g, ' ').trim(),
    );

    expect(lines).toEqual(['Kreissäge users.pages.openSession.toolTotal']);
  });
});
