import { Component, computed, inject, input } from '@angular/core';

import { MatDialogRef } from '@angular/material/dialog';

import { WorkshopSessionApiModel } from '../../api/workshop-sessions/workshop-session.api-model';
import { WorkshopSessionsApiService } from '../../api/workshop-sessions/workshop-sessions.api-service';
import { CloseSessionDialogModalComponent, CloseSessionViewModel } from './close-session-dialog-modal.component';

@Component({
  selector: 'schbar-close-session-dialog-container',
  templateUrl: './close-session-dialog-container.component.html',
  imports: [CloseSessionDialogModalComponent],
})
export class CloseSessionDialogContainerComponent {
  private readonly dialogRef = inject(MatDialogRef<CloseSessionDialogContainerComponent, boolean>);

  readonly customerId = input.required<string>();

  private readonly sessionResource = inject(WorkshopSessionsApiService).openSessionResource(this.customerId);

  protected readonly isLoading = computed(() => this.sessionResource.isLoading());
  protected readonly hasLoadError = computed(() => !!this.sessionResource.error());
  protected readonly viewModel = computed(() => {
    const session = this.sessionResource.hasValue() ? this.sessionResource.value() : null;
    return session ? toCloseSessionViewModel(session) : null;
  });

  protected onConfirmed(): void {
    this.dialogRef.close(true);
  }

  protected onCancelled(): void {
    this.dialogRef.close(false);
  }
}

function toCloseSessionViewModel(session: WorkshopSessionApiModel): CloseSessionViewModel {
  return {
    workshop: { minutes: session.totalWorkshopDurationInMinutes, units: session.totalWorkshopUnits },
    tools: session.toolUsageSummaries.map((summary) => ({
      toolId: summary.toolId,
      name: summary.toolName,
      minutes: summary.totalDurationInMinutes,
      units: summary.totalUnits,
    })),
    hasRunningUsage: hasRunningUsage(session),
  };
}

function hasRunningUsage(session: WorkshopSessionApiModel): boolean {
  return (
    session.workshopUsages.some((usage) => usage.exitTime === null) ||
    session.toolUsageSummaries.some((summary) => summary.active)
  );
}
