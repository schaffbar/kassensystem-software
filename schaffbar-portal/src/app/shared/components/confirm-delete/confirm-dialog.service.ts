import { inject, Service } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';

import { map, Observable } from 'rxjs';

import { ConfirmDeleteComponent, ConfirmDeleteDialogData } from './confirm-delete.component';

/** Opens the confirmation dialog for destructive or important actions (delete, pause, revoke, …). */
@Service()
export class ConfirmDialogService {
  private readonly dialog = inject(MatDialog);

  confirm(data: ConfirmDeleteDialogData): Observable<boolean> {
    return this.dialog
      .open<ConfirmDeleteComponent, ConfirmDeleteDialogData, boolean>(ConfirmDeleteComponent, {
        data,
        minWidth: '600px',
        disableClose: true,
      })
      .afterClosed()
      .pipe(map((confirmed) => confirmed === true));
  }
}
