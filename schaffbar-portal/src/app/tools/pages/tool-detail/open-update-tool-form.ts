import { MatDialog } from '@angular/material/dialog';

import { Observable } from 'rxjs';

import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import {
  UpdateToolDialogData,
  UpdateToolFormComponent,
} from '../../components/update-tool-form/update-tool-form.component';
import { UpdateToolCommand } from '../../tool.model';

// the page header and the info card of the details tab both edit the tool
export function openUpdateToolForm(
  dialog: MatDialog,
  data: UpdateToolDialogData,
): Observable<UpdateToolCommand | undefined> {
  return dialog
    .open<UpdateToolFormComponent, UpdateToolDialogData, UpdateToolCommand>(UpdateToolFormComponent, {
      ...FORM_DIALOG_CONFIG,
      minWidth: DIALOG_WIDTH.medium,
      data,
    })
    .afterClosed();
}
