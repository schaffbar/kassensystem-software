import { describe, expect, it } from 'vitest';

import { AgeGroup } from '../models/age-group';
import { AgeGroupKeyPipe } from './age-group-key.pipe';

describe('AgeGroupKeyPipe', () => {
  const pipe = new AgeGroupKeyPipe();

  it('should return the translation key of the age group', () => {
    expect(pipe.transform(AgeGroup.Under16)).toBe('shared.ageGroups.under_16');
    expect(pipe.transform(AgeGroup.Adult)).toBe('shared.ageGroups.adult');
  });
});
