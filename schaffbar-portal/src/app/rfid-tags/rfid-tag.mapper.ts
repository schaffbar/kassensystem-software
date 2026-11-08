import { parseISO } from 'date-fns';

import { CustomerApiModel } from '../shared/api/customers/customer.api-model';
import {
  RfidTagAssignmentApiModel,
  UnassignRfidTagRequest,
} from '../shared/api/rfid-tag-assignments/rfid-tag-assignment.api-model';
import { RfidTagApiModel } from '../shared/api/rfid-tags/rfid-tag.api-model';
import { RfidTagAssignmentStatus } from '../shared/models/rfid-tag-assignment-status';
import { RfidTag, RfidTagAssignee, UnassignRfidTagCommand, WaitingUser } from './rfid-tag.model';

// ----------------------------------------------------------------------------
// API model → feature model

export function toRfidTags(
  rfidTags: RfidTagApiModel[],
  assignments: RfidTagAssignmentApiModel[],
  customers: CustomerApiModel[],
): RfidTag[] {
  const userNames = toUserNames(customers);
  const assignmentsByRfidTagId = new Map(
    assignments
      .filter((assignment) => assignment.status === RfidTagAssignmentStatus.Assigned && assignment.rfidTagId)
      .map((assignment) => [assignment.rfidTagId, assignment]),
  );

  return rfidTags.map((rfidTag) => {
    const assignment = assignmentsByRfidTagId.get(rfidTag.id);
    const assignee = assignment ? toRfidTagAssignee(assignment, userNames) : null;
    return {
      id: rfidTag.id,
      state: !rfidTag.active ? 'deactivated' : assignee ? 'assigned' : 'free',
      assignee,
    };
  });
}

export function toWaitingUsers(assignments: RfidTagAssignmentApiModel[], customers: CustomerApiModel[]): WaitingUser[] {
  const userNames = toUserNames(customers);
  return assignments
    .filter((assignment) => assignment.status === RfidTagAssignmentStatus.WaitingForAssignment)
    .map((assignment) => ({
      userId: assignment.customerId,
      userName: userNames.get(assignment.customerId) ?? '',
      assignmentType: assignment.assignmentType,
    }));
}

function toRfidTagAssignee(assignment: RfidTagAssignmentApiModel, userNames: Map<string, string>): RfidTagAssignee {
  return {
    userId: assignment.customerId,
    userName: userNames.get(assignment.customerId) ?? '',
    assignmentType: assignment.assignmentType,
    assignedAt: assignment.assignmentDate ? parseISO(assignment.assignmentDate) : null,
  };
}

function toUserNames(customers: CustomerApiModel[]): Map<string, string> {
  return new Map(customers.map((customer) => [customer.id, `${customer.firstName} ${customer.lastName}`]));
}

// ----------------------------------------------------------------------------
// command → request

export function toUnassignRfidTagRequest(command: UnassignRfidTagCommand): UnassignRfidTagRequest {
  return {
    customerId: command.userId,
  };
}
