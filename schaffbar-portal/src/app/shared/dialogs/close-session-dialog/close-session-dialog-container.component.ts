import { Component, computed, inject, input } from '@angular/core';

import { MatDialogRef } from '@angular/material/dialog';

import {
  CloseWorkshopSessionCommand,
  hasRunningUsage,
  WorkshopSession,
} from '../../../workshop-sessions/workshop-session.model';
import { WorkshopSessionService } from '../../../workshop-sessions/workshop-session.service';
import { CloseSessionDialogModalComponent, CloseSessionViewModel } from './close-session-dialog-modal.component';

@Component({
  selector: 'schbar-close-session-dialog-container',
  templateUrl: './close-session-dialog-container.component.html',
  imports: [CloseSessionDialogModalComponent],
})
export class CloseSessionDialogContainerComponent {
  private readonly dialogRef = inject(MatDialogRef<CloseSessionDialogContainerComponent, CloseWorkshopSessionCommand>);

  readonly customerId = input.required<string>();

  private readonly sessionResource = inject(WorkshopSessionService).openSessionResource(this.customerId);

  protected readonly isLoading = computed(() => this.sessionResource.isLoading());
  protected readonly hasLoadError = computed(() => !!this.sessionResource.error());
  protected readonly viewModel = computed(() => {
    const session = this.sessionResource.hasValue() ? this.sessionResource.value() : null;
    return session ? toCloseSessionViewModel(session) : null;
  });

  protected onCloseConfirmed(command: CloseWorkshopSessionCommand): void {
    this.dialogRef.close(command);
  }

  protected onCancelled(): void {
    this.dialogRef.close();
  }
}

function toCloseSessionViewModel(session: WorkshopSession): CloseSessionViewModel {
  return {
    customerId: session.customerId,
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
