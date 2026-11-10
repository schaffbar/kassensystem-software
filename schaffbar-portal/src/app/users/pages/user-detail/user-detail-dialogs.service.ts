import { inject, inputBinding, outputBinding, Service } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';

import { CloseSessionDialogService } from '../../../shared/dialogs/close-session-dialog/close-session-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { AssignRfidTagDialogComponent } from '../../components/assign-rfid-tag-dialog/assign-rfid-tag-dialog.component';
import {
  UpdateUserDialogData,
  UpdateUserFormComponent,
} from '../../components/update-user-form/update-user-form.component';
import { AssignRfidTagCommand } from '../../rfid-tag-assignment.model';
import { DeleteUserCommand, UpdateUserCommand } from '../../user.model';
import { CloseWorkshopSessionCommand } from '../../workshop-session.model';
import { UserDetailStore } from './user-detail.store';

/**
 * The dialogs of the user page – the header, its menu, the banner and the tabs open the same ones. Provided by the
 * page next to its store.
 */
@Service({ autoProvided: false })
export class UserDetailDialogsService {
  private readonly dialog = inject(MatDialog);
  private readonly closeSessionDialog = inject(CloseSessionDialogService);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);
  private readonly store = inject(UserDetailStore);

  editUser(): void {
    const user = this.store.user();
    if (!user) return;

    this.dialog
      .open<UpdateUserFormComponent, UpdateUserDialogData, UpdateUserCommand>(UpdateUserFormComponent, {
        ...FORM_DIALOG_CONFIG,
        minWidth: DIALOG_WIDTH.medium,
        data: { user },
      })
      .afterClosed()
      .subscribe((command) => {
        if (command) {
          this.store.updateUser(command);
        }
      });
  }

  // stays open while the tag is requested and held to the reader: the dialog follows the assignment of the store
  assignRfidTag(): void {
    const user = this.store.user();
    if (!user) return;

    this.dialog.open<AssignRfidTagDialogComponent, void, void>(AssignRfidTagDialogComponent, {
      ...FORM_DIALOG_CONFIG,
      minWidth: DIALOG_WIDTH.medium,
      bindings: [
        inputBinding('userId', () => user.id),
        inputBinding('userName', () => `${user.firstName} ${user.lastName}`),
        inputBinding('rfidTagAssignment', this.store.rfidTagAssignment),
        inputBinding('requestPending', this.store.assignRfidTag.isPending),
        outputBinding<AssignRfidTagCommand>('assignmentRequested', (command) => this.store.assignRfidTag(command)),
      ],
    });
  }

  closeWorkshopSession(): void {
    const user = this.store.user();
    if (!user) return;

    this.closeSessionDialog.open(user.id, `${user.firstName} ${user.lastName}`).subscribe((confirmed) => {
      if (confirmed) {
        const command: CloseWorkshopSessionCommand = { userId: user.id };
        this.store.closeWorkshopSession(command);
      } else {
        // the dialog loaded the session itself and may have shown a newer state than the page
        this.store.reloadOpenSession();
      }
    });
  }

  deleteUser(): void {
    const user = this.store.user();
    if (!user) return;

    this.confirmDeleteDialog
      .open({
        title: 'users.dialogs.deleteUser.title',
        entity: `${user.firstName} ${user.lastName}`,
        message: 'users.dialogs.deleteUser.message',
        confirmLabel: 'users.dialogs.deleteUser.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteUserCommand = { userId: user.id };
          this.store.deleteUser(command);
        }
      });
  }
}
