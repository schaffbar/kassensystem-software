import { DatePipe } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { RfidTagIdPipe } from '../../../shared/pipes/rfid-tag-id.pipe';
import { RfidTagAssignmentTypeKeyPipe } from '../../rfid-tag-assignment-type-key.pipe';
import { UnassignRfidTagCommand } from '../../rfid-tag-assignment.model';

export interface UserRfidTagCardViewModel {
  userId: string;
  userName: string;
  // null = no tag and none requested
  assignment: {
    assignmentType: RfidTagAssignmentType;
    status: RfidTagAssignmentStatus;
    // null while waiting for the tag at the counter
    rfidTagId: string | null;
    assignmentDate: Date | null;
  } | null;
}

@Component({
  selector: 'schbar-user-rfid-tag-card',
  templateUrl: './user-rfid-tag-card.component.html',
  styleUrl: './user-rfid-tag-card.component.scss',
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    OrDashPipe,
    RfidTagIdPipe,
    RfidTagAssignmentTypeKeyPipe,
  ],
  host: { class: 'card' },
})
export class UserRfidTagCardComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly userRfidTagCard = input.required<UserRfidTagCardViewModel>();

  // opens the dialog that requests a tag – or shows the request that waits for the tag
  readonly assignmentRequested = output<void>();
  readonly rfidTagUnassigned = output<UnassignRfidTagCommand>();

  protected readonly RfidTagAssignmentStatus = RfidTagAssignmentStatus;

  // --------------------------------------------------------------------------
  // actions

  protected unassignRfidTag(): void {
    const { userId, userName } = this.userRfidTagCard();
    this.confirmActionDialog
      .open({
        title: 'users.dialogs.unassignRfidTag.title',
        message: 'users.dialogs.unassignRfidTag.message',
        entity: userName,
        confirmLabel: 'users.dialogs.unassignRfidTag.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: UnassignRfidTagCommand = { userId };
          this.rfidTagUnassigned.emit(command);
        }
      });
  }
}
