import { inject, inputBinding, outputBinding, Service } from '@angular/core';

import { MatDialog, MatDialogRef } from '@angular/material/dialog';

import { map, Observable } from 'rxjs';

import { DIALOG_WIDTH } from '../dialog-constants';
import {
  ConfirmDeleteDialogModalComponent,
  ConfirmDeleteDialogViewModel,
} from './confirm-delete-dialog-modal.component';

@Service()
export class ConfirmDeleteDialogService {
  private readonly dialog = inject(MatDialog);

  open(viewModel: ConfirmDeleteDialogViewModel): Observable<boolean> {
    const dialogRef: MatDialogRef<ConfirmDeleteDialogModalComponent, boolean> = this.dialog.open<
      ConfirmDeleteDialogModalComponent,
      void,
      boolean
    >(ConfirmDeleteDialogModalComponent, {
      bindings: [
        inputBinding('viewModel', () => viewModel),
        outputBinding('confirmed', () => dialogRef.close(true)),
        outputBinding('cancelled', () => dialogRef.close(false)),
      ],
      minWidth: DIALOG_WIDTH.medium,
      disableClose: true,
    });

    return dialogRef.afterClosed().pipe(map((confirmed) => confirmed === true));
  }
}
