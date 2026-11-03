import { Component, computed, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TranslatePipe } from '@ngx-translate/core';

import { CloseWorkshopSessionCommand } from '../../../users/shared/models/workshop-session.model';
import { OrDashPipe } from '../../pipes/or-dash.pipe';

export interface CloseSessionViewModel {
  customerId: string;
  workshop: { minutes: number; units: number };
  tools: { toolId: string; name: string | null; minutes: number; units: number }[];
  hasRunningUsage: boolean;
}

@Component({
  selector: 'schbar-close-session-dialog-modal',
  templateUrl: './close-session-dialog-modal.component.html',
  styleUrl: './close-session-dialog-modal.component.scss',
  imports: [
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe,
    OrDashPipe,
  ],
})
export class CloseSessionDialogModalComponent {
  readonly isLoading = input(false);
  readonly hasLoadError = input(false);
  // null: there is no open session (any more)
  readonly viewModel = input.required<CloseSessionViewModel | null>();

  readonly closeConfirmed = output<CloseWorkshopSessionCommand>();
  readonly cancelled = output<void>();

  protected readonly canClose = computed(() => {
    const viewModel = this.viewModel();
    return !this.isLoading() && !this.hasLoadError() && !!viewModel && !viewModel.hasRunningUsage;
  });

  protected onConfirm(): void {
    const viewModel = this.viewModel();
    if (!viewModel) return;

    const command: CloseWorkshopSessionCommand = { customerId: viewModel.customerId };
    this.closeConfirmed.emit(command);
  }
}
