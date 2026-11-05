import { describe, expect, it } from 'vitest';

import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';
import { RfidTagAssignmentTypeKeyPipe } from './rfid-tag-assignment-type-key.pipe';

describe('RfidTagAssignmentTypeKeyPipe', () => {
  const pipe = new RfidTagAssignmentTypeKeyPipe();

  it('should return the translation key of the assignment type', () => {
    expect(pipe.transform(RfidTagAssignmentType.Fixed)).toBe('users.chips.userDetails.rfidTags.fixed');
    expect(pipe.transform(RfidTagAssignmentType.Temporary)).toBe('users.chips.userDetails.rfidTags.temporary');
  });
});
