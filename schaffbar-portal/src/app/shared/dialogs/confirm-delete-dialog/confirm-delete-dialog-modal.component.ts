import { Component, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';

import { TranslatePipe } from '@ngx-translate/core';

export interface ConfirmDeleteDialogViewModel {
  // translation keys
  title: string;
  message: string;
  // plain text
  entity?: string;
}

@Component({
  selector: 'schbar-confirm-delete-dialog-modal',
  templateUrl: './confirm-delete-dialog-modal.component.html',
  imports: [MatButtonModule, MatDialogModule, MatDividerModule, TranslatePipe],
})
export class ConfirmDeleteDialogModalComponent {
  readonly viewModel = input.required<ConfirmDeleteDialogViewModel>();

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
