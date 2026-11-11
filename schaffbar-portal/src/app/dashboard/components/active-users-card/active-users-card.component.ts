import { Component, computed, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';
import { differenceInMinutes } from 'date-fns';
import { interval, map } from 'rxjs';

import { ROUTE, USER_DETAIL_TAB } from '../../../app.routes';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { AgeGroup } from '../../../shared/models/age-group';
import { AgeGroupKeyPipe } from '../../../shared/pipes/age-group-key.pipe';
import { TimeWithDayPipe } from '../../../shared/pipes/time-with-day.pipe';

const DURATION_REFRESH_MS = 60_000;

export interface ActiveUserRowViewModel {
  customerId: string;
  firstName: string;
  lastName: string;
  entryTime: Date;
  // null while the users are not loaded yet
  ageGroup: AgeGroup | null;
  clubMember: boolean;
  // the tool the person has switched on; null if none
  toolName: string | null;
  // checked in on an earlier day and not checked out since
  stayedOvernight: boolean;
}

@Component({
  selector: 'schbar-active-users-card',
  templateUrl: './active-users-card.component.html',
  styleUrl: './active-users-card.component.scss',
  imports: [
    RouterLink,
    MatIconModule,
    TranslatePipe,
    AgeGroupKeyPipe,
    TimeWithDayPipe,
    EmptyStateComponent,
    StatusBadgeComponent,
  ],
  host: { class: 'table-card' },
})
export class ActiveUsersCardComponent {
  readonly activeUsers = input.required<ActiveUserRowViewModel[]>();

  protected readonly usersLink = `/${ROUTE.USERS}`;
  // at the counter the session of a person is what matters – the profile opens on it
  protected readonly openSessionTab = { tab: USER_DETAIL_TAB.OPEN_SESSION };

  // ticks every minute, so the durations stay up to date (the interval ends with the component)
  private readonly now = toSignal(interval(DURATION_REFRESH_MS).pipe(map(() => new Date())), {
    initialValue: new Date(),
  });

  // the longest in the workshop first
  protected readonly rows = computed(() => {
    const now = this.now();
    return [...this.activeUsers()]
      .sort((a, b) => a.entryTime.getTime() - b.entryTime.getTime())
      .map((user) => ({
        ...user,
        initials: `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase(),
        duration: toDuration(user.entryTime, now),
      }));
  });
}

// ----------------------------------------------------------------------------
// helper

function toDuration(entryTime: Date, now: Date): { hours: number; minutes: number } {
  const totalMinutes = Math.max(0, differenceInMinutes(now, entryTime));
  return { hours: Math.floor(totalMinutes / 60), minutes: totalMinutes % 60 };
}
