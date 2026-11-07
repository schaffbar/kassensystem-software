import { Component, inject, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { AgeGroup } from '../../../shared/models/age-group';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { AgeGroupKeyPipe } from '../../age-group-key.pipe';
import { RfidTagAssignmentTypeKeyPipe } from '../../rfid-tag-assignment-type-key.pipe';
import { AssignRfidTagCommand, UnassignRfidTagCommand } from '../../rfid-tag-assignment.model';

export interface UserDetailHeaderViewModel {
  id: string;
  firstName: string;
  lastName: string;
  ageGroup: AgeGroup;
  clubMember: boolean;
}

export interface RfidTagAssignmentViewModel {
  assignmentType: RfidTagAssignmentType;
  status: RfidTagAssignmentStatus;
  // null while waiting for the tag to be scanned
  rfidTagId: string | null;
}

@Component({
  selector: 'schbar-user-detail-header',
  templateUrl: './user-detail-header.component.html',
  styleUrl: './user-detail-header.component.scss',
  imports: [
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    TranslatePipe,
    AgeGroupKeyPipe,
    RfidTagAssignmentTypeKeyPipe,
  ],
})
export class UserDetailHeaderComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly userDetailHeader = input.required<UserDetailHeaderViewModel>();
  // null = no RFID tag
  readonly rfidTagAssignment = input.required<RfidTagAssignmentViewModel | null>();

  readonly editRequested = output<void>();
  readonly reloadRequested = output<void>();
  readonly rfidTagAssigned = output<AssignRfidTagCommand>();
  readonly rfidTagUnassigned = output<UnassignRfidTagCommand>();

  protected readonly RfidTagAssignmentStatus = RfidTagAssignmentStatus;
  protected readonly RfidTagAssignmentType = RfidTagAssignmentType;

  // --------------------------------------------------------------------------
  // actions

  protected assignRfidTag(assignmentType: RfidTagAssignmentType): void {
    const command: AssignRfidTagCommand = { userId: this.userDetailHeader().id, assignmentType };
    this.rfidTagAssigned.emit(command);
  }

  protected unassignRfidTag(): void {
    const user = this.userDetailHeader();
    this.confirmActionDialog
      .open({
        title: 'users.dialogs.unassignRfidTag.title',
        message: 'users.dialogs.unassignRfidTag.message',
        entity: `${user.firstName} ${user.lastName}`,
        confirmLabel: 'users.dialogs.unassignRfidTag.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: UnassignRfidTagCommand = { userId: user.id };
          this.rfidTagUnassigned.emit(command);
        }
      });
  }
}
