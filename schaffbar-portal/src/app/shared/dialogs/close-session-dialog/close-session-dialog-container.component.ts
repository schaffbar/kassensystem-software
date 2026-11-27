import { Component, computed, inject, input } from '@angular/core';

import { MatDialogRef } from '@angular/material/dialog';

import { isSameDay, parseISO } from 'date-fns';

import {
  ToolUsageSummaryApiModel,
  WorkshopSessionApiModel,
} from '../../api/workshop-sessions/workshop-session.api-model';
import { WorkshopSessionsApiService } from '../../api/workshop-sessions/workshop-sessions.api-service';
import {
  CloseSessionDialogModalComponent,
  CloseSessionDialogViewModel,
  CloseSessionItemViewModel,
  CloseSessionToolViewModel,
} from './close-session-dialog-modal.component';

@Component({
  selector: 'schbar-close-session-dialog-container',
  templateUrl: './close-session-dialog-container.component.html',
  imports: [CloseSessionDialogModalComponent],
})
export class CloseSessionDialogContainerComponent {
  private readonly dialogRef = inject(MatDialogRef<CloseSessionDialogContainerComponent, boolean>);

  readonly customerId = input.required<string>();
  readonly userName = input.required<string>();

  private readonly sessionResource = inject(WorkshopSessionsApiService).openWorkshopSessionResource(this.customerId);

  protected readonly isLoading = computed(() => this.sessionResource.isLoading());
  protected readonly hasLoadError = computed(() => !!this.sessionResource.error());
  protected readonly viewModel = computed(() => {
    const session = this.sessionResource.hasValue() ? this.sessionResource.value() : null;
    return session ? toCloseSessionDialogViewModel(session) : null;
  });

  // --------------------------------------------------------------------------
  // actions

  protected closeConfirmed(): void {
    this.dialogRef.close(true);
  }

  protected closeCancelled(): void {
    this.dialogRef.close(false);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toCloseSessionDialogViewModel(session: WorkshopSessionApiModel): CloseSessionDialogViewModel {
  const entryTimes = session.workshopUsages.map((usage) => parseISO(usage.entryTime));
  const exitTimes = session.workshopUsages.map((usage) => usage.exitTime);
  const workshop: CloseSessionItemViewModel = {
    startTime: earliest(entryTimes) ?? parseISO(session.startTime),
    endTime: exitTimes.includes(null) ? null : latest(toDates(exitTimes)),
    minutes: session.totalWorkshopDurationInMinutes,
    units: session.totalWorkshopUnits,
  };
  const tools = session.toolUsageSummaries.map(toCloseSessionToolViewModel);
  return {
    startTime: parseISO(session.startTime),
    workshop,
    tools,
    spansSeveralDays: spansSeveralDays([workshop, ...tools]),
    hasRunningUsage: hasRunningUsage(session),
  };
}

function toCloseSessionToolViewModel(summary: ToolUsageSummaryApiModel): CloseSessionToolViewModel {
  const startTimes = summary.usages.map((usage) => parseISO(usage.startTime));
  const endTimes = summary.usages.map((usage) => usage.endTime);
  return {
    toolId: summary.toolId,
    name: summary.toolName,
    startTime: earliest(startTimes) ?? new Date(),
    endTime: summary.running || endTimes.includes(null) ? null : latest(toDates(endTimes)),
    minutes: summary.totalDurationInMinutes,
    units: summary.totalUnits,
  };
}

// ----------------------------------------------------------------------------
// helper

function hasRunningUsage(session: WorkshopSessionApiModel): boolean {
  return (
    session.workshopUsages.some((usage) => usage.exitTime === null) ||
    session.toolUsageSummaries.some((summary) => summary.running)
  );
}

// the end of a running usage ("now") does not count – the row shows "now" instead of a time
function spansSeveralDays(items: CloseSessionItemViewModel[]): boolean {
  const times = items.flatMap((item) => [item.startTime, item.endTime]).filter((time) => time !== null);
  return times.some((time) => !isSameDay(time, times[0]));
}

function toDates(times: (string | null)[]): Date[] {
  return times.filter((time) => time !== null).map((time) => parseISO(time));
}

function earliest(dates: Date[]): Date | null {
  return dates.length > 0 ? new Date(Math.min(...dates.map((date) => date.getTime()))) : null;
}

function latest(dates: Date[]): Date | null {
  return dates.length > 0 ? new Date(Math.max(...dates.map((date) => date.getTime()))) : null;
}
