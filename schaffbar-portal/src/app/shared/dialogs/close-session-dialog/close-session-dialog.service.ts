import { inject, inputBinding, Service } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';

import { Observable } from 'rxjs';

import { CloseWorkshopSessionCommand } from '../../../users/shared/models/workshop-session.model';
import { DIALOG_WIDTH } from '../dialog-constants';
import { CloseSessionDialogContainerComponent } from './close-session-dialog-container.component';

@Service()
export class CloseSessionDialogService {
  private readonly dialog = inject(MatDialog);

  open(customerId: string): Observable<CloseWorkshopSessionCommand | undefined> {
    return this.dialog
      .open<CloseSessionDialogContainerComponent, void, CloseWorkshopSessionCommand>(
        CloseSessionDialogContainerComponent,
        {
          bindings: [inputBinding('customerId', () => customerId)],
          minWidth: DIALOG_WIDTH.medium,
          disableClose: true,
        },
      )
      .afterClosed();
  }
}
