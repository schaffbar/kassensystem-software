import { describe, expect, it } from 'vitest';

import { customerFixture } from '../shared/api/customers/customer.api-fixtures';
import { RfidTagAssignmentApiModel } from '../shared/api/rfid-tag-assignments/rfid-tag-assignment.api-model';
import { RfidTagAssignmentStatus } from '../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';
import { toRfidTags, toUnassignRfidTagRequest, toWaitingUser } from './rfid-tag.mapper';

describe('rfid-tag.mapper', () => {
  const anna = customerFixture({ id: 'user-1', firstName: 'Anna', lastName: 'Becker' });
  const max = customerFixture({ id: 'user-2', firstName: 'Max', lastName: 'Schulz' });
  const assigned: RfidTagAssignmentApiModel = {
    id: 'assignment-1',
    customerId: 'user-1',
    assignmentType: RfidTagAssignmentType.Fixed,
    status: RfidTagAssignmentStatus.Assigned,
    rfidTagId: '04A31F9C',
    assignmentDate: '2025-02-03T10:00:00Z',
  };
  const waiting: RfidTagAssignmentApiModel = {
    id: 'assignment-2',
    customerId: 'user-2',
    assignmentType: RfidTagAssignmentType.Temporary,
    status: RfidTagAssignmentStatus.WaitingForAssignment,
    rfidTagId: null,
    assignmentDate: null,
  };

  describe('toRfidTags', () => {
    it('should add the person a tag is assigned to', () => {
      const [rfidTag] = toRfidTags([{ id: '04A31F9C', active: true }], [assigned, waiting], [anna, max]);

      expect(rfidTag).toEqual({
        id: '04A31F9C',
        state: 'assigned',
        assignee: {
          userId: 'user-1',
          userName: 'Anna Becker',
          assignmentType: RfidTagAssignmentType.Fixed,
          assignedAt: new Date('2025-02-03T10:00:00Z'),
        },
      });
    });

    it('should call an active tag without assignment free and an inactive one deactivated', () => {
      const rfidTags = toRfidTags(
        [
          { id: 'C385A62C', active: true },
          { id: '04D56B11', active: false },
        ],
        [waiting],
        [anna, max],
      );

      expect(rfidTags).toEqual([
        { id: 'C385A62C', state: 'free', assignee: null },
        { id: '04D56B11', state: 'deactivated', assignee: null },
      ]);
    });
  });

  describe('toWaitingUser', () => {
    it('should return the person waiting for a tag with the name', () => {
      expect(toWaitingUser([assigned, waiting], [anna, max])).toEqual({
        userId: 'user-2',
        userName: 'Max Schulz',
        assignmentType: RfidTagAssignmentType.Temporary,
      });
    });

    it('should return null when nobody waits', () => {
      expect(toWaitingUser([assigned], [anna, max])).toBeNull();
    });
  });

  describe('toUnassignRfidTagRequest', () => {
    it('should send only the person', () => {
      expect(toUnassignRfidTagRequest({ rfidTagId: '04A31F9C', userId: 'user-1' })).toEqual({ customerId: 'user-1' });
    });
  });
});
