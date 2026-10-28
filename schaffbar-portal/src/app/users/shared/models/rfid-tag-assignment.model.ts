export interface RfidTagAssignment {
  id: string;
  customerId: string;
  rfidTagId: string | null;
  assignmentType: RfidTagAssignmentType;
  status: RfidTagAssignmentStatus;
  assignmentDate: string | null;
}

export enum RfidTagAssignmentStatus {
  WaitingForAssignment = 'WAITING_FOR_ASSIGNMENT',
  Assigned = 'ASSIGNED',
}

export enum RfidTagAssignmentType {
  Fixed = 'FIXED',
  Temporary = 'TEMPORARY',
}

// ----------------------------------------------------------------------------
// commands

export interface AssignRfidTagCommand {
  customerId: string;
  assignmentType: RfidTagAssignmentType;
}

export interface UnassignRfidTagCommand {
  customerId: string;
}
