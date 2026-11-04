import { inject, inputBinding, Service } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';

import { map, Observable } from 'rxjs';

import { DIALOG_WIDTH } from '../dialog-constants';
import { CloseSessionDialogContainerComponent } from './close-session-dialog-container.component';

@Service()
export class CloseSessionDialogService {
  private readonly dialog = inject(MatDialog);

  open(customerId: string): Observable<boolean> {
    return this.dialog
      .open<CloseSessionDialogContainerComponent, void, boolean>(CloseSessionDialogContainerComponent, {
        bindings: [inputBinding('customerId', () => customerId)],
        minWidth: DIALOG_WIDTH.medium,
        disableClose: true,
      })
      .afterClosed()
      .pipe(map((confirmed) => confirmed === true));
  }
}
