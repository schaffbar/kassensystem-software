import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../pipes/or-dash.pipe';
import { TimeWithDayPipe } from '../../pipes/time-with-day.pipe';
import { FormDialogHeaderComponent } from '../form-dialog-header/form-dialog-header.component';

export interface CloseSessionDialogViewModel {
  // first entry of the session
  startTime: Date;
  workshop: CloseSessionItemViewModel;
  tools: CloseSessionToolViewModel[];
  hasRunningUsage: boolean;
}

export interface CloseSessionItemViewModel {
  startTime: Date;
  // null while running
  endTime: Date | null;
  minutes: number;
  units: number;
}

export interface CloseSessionToolViewModel extends CloseSessionItemViewModel {
  toolId: string;
  // null if the tool has been deleted
  name: string | null;
}

@Component({
  selector: 'schbar-close-session-dialog-modal',
  templateUrl: './close-session-dialog-modal.component.html',
  styleUrl: './close-session-dialog-modal.component.scss',
  imports: [
    NgTemplateOutlet,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe,
    OrDashPipe,
    TimeWithDayPipe,
    FormDialogHeaderComponent,
  ],
  host: { class: 'form-dialog' },
})
export class CloseSessionDialogModalComponent {
  // translated – the subtitle names the person
  readonly userName = input.required<string>();
  readonly isLoading = input(false);
  readonly hasLoadError = input(false);
  // null: there is no open session (any more)
  readonly viewModel = input.required<CloseSessionDialogViewModel | null>();

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  protected readonly canClose = computed(() => {
    const viewModel = this.viewModel();
    return !this.isLoading() && !this.hasLoadError() && !!viewModel && !viewModel.hasRunningUsage;
  });
}
