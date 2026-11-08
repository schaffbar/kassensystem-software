import { describe, expect, it } from 'vitest';

import { customerFixture } from '../shared/api/customers/customer.api-fixtures';
import { toUser } from './user.mapper';

describe('toUser', () => {
  it('should take only id, name and e-mail address from the API model', () => {
    expect(toUser(customerFixture({ id: 'user-1', firstName: 'Max', lastName: 'Mustermann' }))).toEqual({
      id: 'user-1',
      firstName: 'Max',
      lastName: 'Mustermann',
      email: 'max@example.org',
    });
  });
});
