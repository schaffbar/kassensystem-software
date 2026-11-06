import { describe, expect, it } from 'vitest';

import { RfidTagAssignmentStatus } from '../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';
import {
  toRequestRfidTagAssignmentRequest,
  toRfidTagAssignment,
  toUnassignRfidTagRequest,
} from './rfid-tag-assignment.mapper';

describe('rfid-tag-assignment.mapper', () => {
  describe('toRfidTagAssignment', () => {
    it('should map an assigned tag and parse the assignment date', () => {
      expect(
        toRfidTagAssignment({
          id: 'a-1',
          customerId: 'user-1',
          assignmentType: RfidTagAssignmentType.Fixed,
          status: RfidTagAssignmentStatus.Assigned,
          rfidTagId: 'tag-1',
          assignmentDate: '2026-10-31T10:00:00Z',
        }),
      ).toEqual({
        id: 'a-1',
        customerId: 'user-1',
        assignmentType: RfidTagAssignmentType.Fixed,
        status: RfidTagAssignmentStatus.Assigned,
        rfidTagId: 'tag-1',
        assignmentDate: new Date('2026-10-31T10:00:00Z'),
      });
    });

    it('should keep tag and date of a waiting assignment as null', () => {
      const assignment = toRfidTagAssignment({
        id: 'a-1',
        customerId: 'user-1',
        assignmentType: RfidTagAssignmentType.Temporary,
        status: RfidTagAssignmentStatus.WaitingForAssignment,
        rfidTagId: null,
        assignmentDate: null,
      });

      expect(assignment).toMatchObject({ rfidTagId: null, assignmentDate: null });
    });
  });

  describe('toRequestRfidTagAssignmentRequest', () => {
    it('should send customer and assignment type', () => {
      expect(
        toRequestRfidTagAssignmentRequest({ userId: 'user-1', assignmentType: RfidTagAssignmentType.Fixed }),
      ).toEqual({ customerId: 'user-1', assignmentType: RfidTagAssignmentType.Fixed });
    });
  });

  describe('toUnassignRfidTagRequest', () => {
    it('should send the customer', () => {
      expect(toUnassignRfidTagRequest({ userId: 'user-1' })).toEqual({ customerId: 'user-1' });
    });
  });
});
