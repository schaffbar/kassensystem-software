import { describe, expect, it } from 'vitest';

import { AgeGroup } from '../shared/models/age-group';
import { AgeGroupKeyPipe } from './age-group-key.pipe';

describe('AgeGroupKeyPipe', () => {
  const pipe = new AgeGroupKeyPipe();

  it('should return the translation key of the age group', () => {
    expect(pipe.transform(AgeGroup.Under16)).toBe('users.chips.userDetails.ageGroup.under_16');
    expect(pipe.transform(AgeGroup.Adult)).toBe('users.chips.userDetails.ageGroup.adult');
  });
});
