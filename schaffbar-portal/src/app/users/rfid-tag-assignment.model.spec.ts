import { describe, expect, it } from 'vitest';

import { RfidTagAssignmentStatus } from '../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';
import { isWaitingForRfidTag, RfidTagAssignment } from './rfid-tag-assignment.model';

describe('rfid-tag-assignment.model', () => {
  const assignment: RfidTagAssignment = {
    id: 'a-1',
    customerId: 'user-1',
    rfidTagId: null,
    assignmentType: RfidTagAssignmentType.Temporary,
    status: RfidTagAssignmentStatus.WaitingForAssignment,
    assignmentDate: null,
  };

  it('should wait for the tag only while the request is open', () => {
    expect(isWaitingForRfidTag(assignment)).toBe(true);
    expect(isWaitingForRfidTag({ ...assignment, status: RfidTagAssignmentStatus.Assigned, rfidTagId: '04A3' })).toBe(
      false,
    );
    expect(isWaitingForRfidTag(null)).toBe(false);
  });
});
