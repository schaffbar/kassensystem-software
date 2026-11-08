import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';

// a deactivated tag is rejected by every reader; an active one is either assigned to a person or free
export type RfidTagState = 'assigned' | 'free' | 'deactivated';

export interface RfidTag {
  id: string;
  state: RfidTagState;
  assignee: RfidTagAssignee | null;
}

// an assigned tag must be unassigned first – otherwise the assignment would point to a tag that no longer exists
export function canDeleteRfidTag(rfidTag: RfidTag): boolean {
  return !rfidTag.assignee;
}

export interface RfidTagAssignee {
  userId: string;
  userName: string;
  assignmentType: RfidTagAssignmentType;
  assignedAt: Date | null;
}

// ----------------------------------------------------------------------------
// waiting for a tag

// a person who asked for a tag at the counter and waits until one is placed on the assigner
export interface WaitingUser {
  userId: string;
  userName: string;
  assignmentType: RfidTagAssignmentType;
}

// ----------------------------------------------------------------------------
// commands

export interface UnassignRfidTagCommand {
  rfidTagId: string;
  userId: string;
}

export interface DeleteRfidTagCommand {
  rfidTagId: string;
}
