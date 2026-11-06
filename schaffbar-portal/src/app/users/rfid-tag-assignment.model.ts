import { RfidTagAssignmentStatus } from '../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';

export interface RfidTagAssignment {
  id: string;
  customerId: string;
  rfidTagId: string | null;
  assignmentType: RfidTagAssignmentType;
  status: RfidTagAssignmentStatus;
  assignmentDate: Date | null;
}

// ----------------------------------------------------------------------------
// commands

export interface AssignRfidTagCommand {
  userId: string;
  assignmentType: RfidTagAssignmentType;
}

export interface UnassignRfidTagCommand {
  userId: string;
}
