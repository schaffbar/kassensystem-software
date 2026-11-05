import { AgeGroup } from '../../models/age-group';
import { CustomerApiModel } from './customer.api-model';

export function customerFixture(overrides: Partial<CustomerApiModel> = {}): CustomerApiModel {
  return {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: '1990-01-31',
    ageGroup: AgeGroup.Adult,
    clubMember: false,
    email: 'max@example.org',
    phone: null,
    address: {
      addressLine1: 'Hauptstraße 1',
      addressLine2: null,
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    },
    createdAt: '2026-01-01T10:00:00Z',
    updatedAt: '2026-01-01T10:00:00Z',
    ...overrides,
  };
}
