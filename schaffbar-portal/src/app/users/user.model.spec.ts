import { describe, expect, it } from 'vitest';

import { AgeGroup } from '../shared/models/age-group';
import { hasAddressChanged, hasContactChanged, hasPersonChanged, UpdateUserCommand, User } from './user.model';

describe('user.model', () => {
  const user: User = {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1990, 0, 31),
    ageGroup: AgeGroup.Adult,
    clubMember: false,
    email: 'max@example.org',
    phone: null,
    address: { addressLine1: 'Hauptstraße 1', addressLine2: null, postalCode: '12345', city: 'Berlin', country: 'D' },
    createdAt: new Date(2026, 0, 1),
    updatedAt: new Date(2026, 0, 1),
  };
  // the dialog sends missing optional values as undefined
  const unchanged: UpdateUserCommand = {
    userId: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1990, 0, 31),
    clubMember: false,
    email: 'max@example.org',
    addressLine1: 'Hauptstraße 1',
    postalCode: '12345',
    city: 'Berlin',
    country: 'D',
  };

  it('should see no change in an unchanged user, with null and undefined for missing values', () => {
    expect(hasPersonChanged(user, unchanged)).toBe(false);
    expect(hasContactChanged(user, unchanged)).toBe(false);
    expect(hasAddressChanged(user, unchanged)).toBe(false);
  });

  it('should compare the date of birth by day', () => {
    expect(hasPersonChanged(user, { ...unchanged, dateOfBirth: new Date(1990, 0, 31, 12) })).toBe(false);
    expect(hasPersonChanged(user, { ...unchanged, dateOfBirth: new Date(1990, 1, 1) })).toBe(true);
  });

  it('should tell which part changed', () => {
    expect(hasPersonChanged(user, { ...unchanged, clubMember: true })).toBe(true);
    expect(hasContactChanged(user, { ...unchanged, phone: '0123' })).toBe(true);
    expect(hasAddressChanged(user, { ...unchanged, addressLine2: 'Hinterhaus' })).toBe(true);
    expect(hasContactChanged(user, { ...unchanged, city: 'Potsdam' })).toBe(false);
  });
});
