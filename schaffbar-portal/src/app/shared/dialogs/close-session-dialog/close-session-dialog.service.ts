import { inject, inputBinding, Service } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';

import { map, Observable } from 'rxjs';

import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../dialog-constants';
import { CloseSessionDialogContainerComponent } from './close-session-dialog-container.component';

@Service()
export class CloseSessionDialogService {
  private readonly dialog = inject(MatDialog);

  // userName: shown in the header, the dialog loads only the session
  open(customerId: string, userName: string): Observable<boolean> {
    return this.dialog
      .open<CloseSessionDialogContainerComponent, void, boolean>(CloseSessionDialogContainerComponent, {
        bindings: [inputBinding('customerId', () => customerId), inputBinding('userName', () => userName)],
        position: FORM_DIALOG_CONFIG.position,
        minWidth: DIALOG_WIDTH.medium,
        disableClose: true,
      })
      .afterClosed()
      .pipe(map((confirmed) => confirmed === true));
  }
}
