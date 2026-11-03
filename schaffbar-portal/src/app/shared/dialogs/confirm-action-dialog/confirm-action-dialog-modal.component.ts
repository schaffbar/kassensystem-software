import { Component, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';

import { TranslatePipe } from '@ngx-translate/core';

export interface ConfirmActionDialogViewModel {
  // translation keys
  title: string;
  message: string;
  confirmLabel: string;
  // plain text
  entity?: string;
  destructive: boolean;
}

@Component({
  selector: 'schbar-confirm-action-dialog-modal',
  templateUrl: './confirm-action-dialog-modal.component.html',
  imports: [MatButtonModule, MatDialogModule, MatDividerModule, TranslatePipe],
})
export class ConfirmActionDialogModalComponent {
  readonly viewModel = input.required<ConfirmActionDialogViewModel>();

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
