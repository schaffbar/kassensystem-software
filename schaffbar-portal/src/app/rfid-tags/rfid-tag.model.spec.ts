import { describe, expect, it } from 'vitest';

import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';
import { canDeleteRfidTag, RfidTag } from './rfid-tag.model';

describe('canDeleteRfidTag', () => {
  const freeTag: RfidTag = { id: '04A31F9C', state: 'free', assignee: null };

  it('should allow deleting a tag without assignment', () => {
    expect(canDeleteRfidTag(freeTag)).toBe(true);
    expect(canDeleteRfidTag({ ...freeTag, state: 'deactivated' })).toBe(true);
  });

  it('should not allow deleting an assigned tag', () => {
    const assignedTag: RfidTag = {
      ...freeTag,
      state: 'assigned',
      assignee: {
        userId: 'user-1',
        userName: 'Anna Becker',
        assignmentType: RfidTagAssignmentType.Fixed,
        assignedAt: null,
      },
    };

    expect(canDeleteRfidTag(assignedTag)).toBe(false);
  });
});
