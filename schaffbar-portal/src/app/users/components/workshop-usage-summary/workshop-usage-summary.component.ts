import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface WorkshopUsageViewModel {
  entryTime: Date;
  // null while the user is still in the workshop
  exitTime: Date | null;
  durationInMinutes: number | null;
  unitsUsed: number | null;
}

@Component({
  selector: 'schbar-workshop-usage-summary',
  templateUrl: './workshop-usage-summary.component.html',
  styleUrl: './workshop-usage-summary.component.scss',
  imports: [MatTableModule, DatePipe, TranslatePipe, OrDashPipe],
})
export class WorkshopUsageSummaryComponent {
  readonly usages = input.required<WorkshopUsageViewModel[]>();
  readonly totalTimeInMinutes = input.required<number>();
  readonly totalUnitsUsed = input.required<number>();

  protected readonly displayedColumns: string[] = ['entryTime', 'exitTime', 'durationInMinutes', 'unitsUsed'];
}
