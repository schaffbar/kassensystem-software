import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface UserOpenSessionViewModel {
  // the stays in the workshop, oldest first
  stays: UserSessionStayViewModel[];
  workshopMinutes: number;
  workshopUnits: number;
  toolUnits: number;
  // one line per tool in the summary
  tools: UserSessionToolTotalViewModel[];
}

// a stay from the entry to the exit, with the tools used during it in the order they started
export interface UserSessionStayViewModel extends UserSessionUsageViewModel {
  toolUsages: UserSessionToolUsageViewModel[];
}

export interface UserSessionToolUsageViewModel extends UserSessionUsageViewModel {
  // null if the tool has been deleted
  toolName: string | null;
}

export interface UserSessionUsageViewModel {
  id: string;
  startTime: Date;
  // null while running – like duration and units, which the backend knows only at the end
  endTime: Date | null;
  durationInMinutes: number | null;
  units: number | null;
}

export interface UserSessionToolTotalViewModel {
  toolId: string;
  // null if the tool has been deleted
  toolName: string | null;
  usages: number;
  minutes: number;
  units: number;
}

@Component({
  selector: 'schbar-user-open-session',
  templateUrl: './user-open-session.component.html',
  styleUrl: './user-open-session.component.scss',
  imports: [DatePipe, MatTableModule, TranslatePipe, OrDashPipe],
})
export class UserOpenSessionComponent {
  readonly userOpenSession = input.required<UserOpenSessionViewModel>();

  protected readonly displayedColumns = ['item', 'startTime', 'endTime', 'duration', 'units'];

  // the table is flat: each stay followed by its tools
  protected readonly rows = computed(() =>
    this.userOpenSession().stays.flatMap((stay): UsageRow[] => [
      { ...stay, kind: 'stay', toolName: null },
      ...stay.toolUsages.map((usage): UsageRow => ({ ...usage, kind: 'tool' })),
    ]),
  );
}

// ----------------------------------------------------------------------------
// helper

interface UsageRow extends UserSessionUsageViewModel {
  kind: 'stay' | 'tool';
  toolName: string | null;
}
