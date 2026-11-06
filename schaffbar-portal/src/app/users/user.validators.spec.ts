import { describe, expect, it } from 'vitest';

import { dateOfBirthRange } from './user.validators';

describe('dateOfBirthRange', () => {
  it('should allow at most 100 years and end with the previous year', () => {
    const { min, max } = dateOfBirthRange(new Date(2026, 10, 6));

    expect(min).toEqual(new Date(2026 - 100, 0, 1));
    expect(max).toEqual(new Date(2025, 11, 31, 23, 59, 59, 999));
  });

  it('should move with the year', () => {
    expect(dateOfBirthRange(new Date(2027, 0, 1)).max).toEqual(new Date(2026, 11, 31, 23, 59, 59, 999));
  });
});
