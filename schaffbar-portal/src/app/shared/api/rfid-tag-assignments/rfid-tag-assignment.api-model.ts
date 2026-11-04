import * as z from 'zod';

import { RfidTagAssignmentStatus } from '../../models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../models/rfid-tag-assignment-type';

export const rfidTagAssignmentSchema = z.object({
  id: z.string(),
  customerId: z.string(),
  assignmentType: z.enum(RfidTagAssignmentType),
  status: z.enum(RfidTagAssignmentStatus),
  rfidTagId: z.string().nullable(),
  assignmentDate: z.iso.datetime().nullable(),
});

export type RfidTagAssignmentApiModel = z.infer<typeof rfidTagAssignmentSchema>;

// ----------------------------------------------------------------------------
// requests

export interface RequestRfidTagAssignmentRequest {
  customerId: string;
  assignmentType: RfidTagAssignmentType;
}

export interface UnassignRfidTagRequest {
  customerId: string;
}
