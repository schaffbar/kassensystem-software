import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface UserOpenSessionViewModel {
  usages: UserSessionUsageRowViewModel[];
  workshopMinutes: number;
  workshopUnits: number;
  toolUnits: number;
}

// a stay in the workshop or the use of a tool
export interface UserSessionUsageRowViewModel {
  id: string;
  kind: 'workshop' | 'tool';
  // tool name – null for the workshop or a deleted tool
  toolName: string | null;
  startTime: Date;
  // null while running
  endTime: Date | null;
  durationInMinutes: number | null;
  units: number | null;
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
}
