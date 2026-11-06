import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface WorkshopUsageRowViewModel {
  entryTime: Date;
  // null while the user is still in the workshop
  exitTime: Date | null;
  durationInMinutes: number | null;
  unitsUsed: number | null;
}

export interface WorkshopUsageSummaryViewModel {
  workshopUsages: WorkshopUsageRowViewModel[];
  totalWorkshopDurationInMinutes: number;
  totalWorkshopUnits: number;
}

@Component({
  selector: 'schbar-workshop-usage-summary',
  templateUrl: './workshop-usage-summary.component.html',
  styleUrl: './workshop-usage-summary.component.scss',
  imports: [MatTableModule, DatePipe, TranslatePipe, OrDashPipe],
})
export class WorkshopUsageSummaryComponent {
  readonly workshopUsageSummary = input.required<WorkshopUsageSummaryViewModel>();

  protected readonly displayedColumns: string[] = ['entryTime', 'exitTime', 'durationInMinutes', 'unitsUsed'];
}
