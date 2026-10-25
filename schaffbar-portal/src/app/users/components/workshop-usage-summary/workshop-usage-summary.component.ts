import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { WorkshopUsage } from '../../shared/models/workshop-session.model';

@Component({
  selector: 'schbar-workshop-usage-summary',
  templateUrl: './workshop-usage-summary.component.html',
  styleUrl: './workshop-usage-summary.component.scss',
  imports: [MatTableModule, DatePipe, TranslatePipe],
})
export class WorkshopUsageSummaryComponent {
  readonly usages = input.required<WorkshopUsage[]>();
  readonly totalTimeInMinutes = input.required<number>();
  readonly totalUnitsUsed = input.required<number>();

  protected readonly displayedColumns: string[] = ['entryTime', 'exitTime', 'durationInMinutes', 'unitsUsed'];
}
