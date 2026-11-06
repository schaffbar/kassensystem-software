import { Component, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

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
    const command: AssignRfidTagCommand = { customerId: this.userDetailHeader().id, assignmentType };
    this.rfidTagAssigned.emit(command);
  }

  protected unassignRfidTag(): void {
    const command: UnassignRfidTagCommand = { customerId: this.userDetailHeader().id };
    this.rfidTagUnassigned.emit(command);
  }
}
