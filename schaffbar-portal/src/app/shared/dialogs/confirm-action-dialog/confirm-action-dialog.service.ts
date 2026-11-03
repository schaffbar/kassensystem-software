import { inject, inputBinding, outputBinding, Service } from '@angular/core';

import { MatDialog, MatDialogRef } from '@angular/material/dialog';

import { map, Observable } from 'rxjs';

import { DIALOG_WIDTH } from '../dialog-constants';
import {
  ConfirmActionDialogModalComponent,
  ConfirmActionDialogViewModel,
} from './confirm-action-dialog-modal.component';

@Service()
export class ConfirmActionDialogService {
  private readonly dialog = inject(MatDialog);

  open(viewModel: ConfirmActionDialogViewModel): Observable<boolean> {
    const dialogRef: MatDialogRef<ConfirmActionDialogModalComponent, boolean> = this.dialog.open<
      ConfirmActionDialogModalComponent,
      void,
      boolean
    >(ConfirmActionDialogModalComponent, {
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
