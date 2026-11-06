import { parseISO } from 'date-fns';

import {
  RequestRfidTagAssignmentRequest,
  RfidTagAssignmentApiModel,
  UnassignRfidTagRequest,
} from '../shared/api/rfid-tag-assignments/rfid-tag-assignment.api-model';
import { AssignRfidTagCommand, RfidTagAssignment, UnassignRfidTagCommand } from './rfid-tag-assignment.model';

// ----------------------------------------------------------------------------
// API model → feature model

export function toRfidTagAssignment(apiModel: RfidTagAssignmentApiModel): RfidTagAssignment {
  return {
    id: apiModel.id,
    customerId: apiModel.customerId,
    rfidTagId: apiModel.rfidTagId,
    assignmentType: apiModel.assignmentType,
    status: apiModel.status,
    assignmentDate: apiModel.assignmentDate ? parseISO(apiModel.assignmentDate) : null,
  };
}

// ----------------------------------------------------------------------------
// command → request

export function toRequestRfidTagAssignmentRequest(command: AssignRfidTagCommand): RequestRfidTagAssignmentRequest {
  return {
    customerId: command.customerId,
    assignmentType: command.assignmentType,
  };
}

export function toUnassignRfidTagRequest(command: UnassignRfidTagCommand): UnassignRfidTagRequest {
  return {
    customerId: command.customerId,
  };
}
