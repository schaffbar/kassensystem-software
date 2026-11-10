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

// requested, but the tag has not been held to the reader at the counter yet
export function isWaitingForRfidTag(assignment: RfidTagAssignment | null): boolean {
  return assignment?.status === RfidTagAssignmentStatus.WaitingForAssignment;
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
