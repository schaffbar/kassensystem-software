import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AgeGroup } from '../../../shared/models/age-group';
import { ActiveUserRowViewModel, ActiveUsersCardComponent } from './active-users-card.component';

registerLocaleData(localeDe);

describe('ActiveUsersCardComponent', () => {
  const at = (hours: number, minutes: number) => new Date(2026, 10, 11, hours, minutes);
  const anna: ActiveUserRowViewModel = {
    customerId: 'user-1',
    firstName: 'Anna',
    lastName: 'Becker',
    entryTime: at(14, 5),
    ageGroup: AgeGroup.Adult,
    clubMember: true,
    toolName: 'Drechselbank',
  };
  const max: ActiveUserRowViewModel = {
    customerId: 'user-2',
    firstName: 'Max',
    lastName: 'Schulz',
    entryTime: at(15, 2),
    ageGroup: AgeGroup.Under18,
    clubMember: false,
    toolName: null,
  };

  let fixture: ComponentFixture<ActiveUsersCardComponent>;

  beforeEach(() => {
    // "now" for the durations – only the clock, the interval of the component stays real
    vi.useFakeTimers({ toFake: ['Date'], now: at(15, 28) });
    TestBed.configureTestingModule({
      imports: [ActiveUsersCardComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    });
    fixture = TestBed.createComponent(ActiveUsersCardComponent);
  });

  afterEach(() => vi.useRealTimers());

  function rows(): string[][] {
    return [...fixture.nativeElement.querySelectorAll('li a')].map((row: HTMLElement) =>
      ['.name', '.meta', 'schbar-status-badge', '.duration', '.since'].map(
        (selector) => row.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '',
      ),
    );
  }

  it('should list the longest in the workshop first, with age group, membership, tool and duration', async () => {
    fixture.componentRef.setInput('activeUsers', [max, anna]);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.count').textContent.trim()).toBe('2');
    expect(rows()).toEqual([
      [
        'Anna Becker',
        'shared.ageGroups.adult · dashboard.activeUsers.clubMember',
        'play_circleDrechselbank',
        'dashboard.activeUsers.durationHours',
        'dashboard.activeUsers.since',
      ],
      [
        'Max Schulz',
        'shared.ageGroups.under_18',
        '',
        'dashboard.activeUsers.durationMinutes',
        'dashboard.activeUsers.since',
      ],
    ]);
  });

  it('should link each person to the open session in the profile', async () => {
    fixture.componentRef.setInput('activeUsers', [anna]);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('li a').getAttribute('href')).toBe('/users/user-1?tab=open-session');
  });

  it('should show an empty state when nobody is in the workshop', async () => {
    fixture.componentRef.setInput('activeUsers', []);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('schbar-empty-state')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('ul')).toBeNull();
  });
});
