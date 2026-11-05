import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface ToolUsageViewModel {
  startTime: Date;
  // null while the tool is still in use
  endTime: Date | null;
  durationInMinutes: number | null;
  unitsUsed: number | null;
}

export interface ToolUsageSummaryViewModel {
  // null if the tool has been deleted
  toolName: string | null;
  active: boolean;
  totalDurationInMinutes: number;
  totalUnits: number;
  usages: ToolUsageViewModel[];
}

@Component({
  selector: 'schbar-tool-usage-summary',
  templateUrl: './tool-usage-summary.component.html',
  styleUrl: './tool-usage-summary.component.scss',
  imports: [MatTableModule, MatChipsModule, DatePipe, TranslatePipe, OrDashPipe],
})
export class ToolUsageSummaryComponent {
  readonly summary = input.required<ToolUsageSummaryViewModel>();

  protected readonly toolUsageColumns: string[] = ['startTime', 'endTime', 'durationInMinutes', 'unitsUsed'];
}
