import { describe, expect, it } from 'vitest';

import { customerFixture } from '../shared/api/customers/customer.api-fixtures';
import { AgeGroup } from '../shared/models/age-group';
import { RfidTagAssignmentStatus } from '../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';
import {
  toCreateCustomerRequest,
  toUpdateCustomerAddressRequest,
  toUpdateCustomerContactRequest,
  toUpdateCustomerRequest,
  toUser,
  toUserListItems,
} from './user.mapper';
import { UpdateUserCommand } from './user.model';

describe('user.mapper', () => {
  describe('toUser', () => {
    it('should map the customer with address and parse the dates', () => {
      expect(toUser(customerFixture())).toEqual({
        id: 'user-1',
        firstName: 'Max',
        lastName: 'Mustermann',
        dateOfBirth: new Date(1990, 0, 31),
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
        createdAt: new Date('2026-01-01T10:00:00Z'),
        updatedAt: new Date('2026-01-01T10:00:00Z'),
      });
    });
  });

  describe('toUserListItems', () => {
    it('should add the tag of the assignment and whether the user is in the workshop', () => {
      const max = customerFixture({ id: 'user-1' });
      const erika = customerFixture({ id: 'user-2', firstName: 'Erika' });
      const otto = customerFixture({ id: 'user-3', firstName: 'Otto' });
      const assignment = {
        id: 'assignment-1',
        customerId: 'user-1',
        assignmentType: RfidTagAssignmentType.Fixed,
        status: RfidTagAssignmentStatus.Assigned,
        rfidTagId: '04A31F9C',
        assignmentDate: '2026-01-02T10:00:00Z',
      };
      const waiting = {
        ...assignment,
        id: 'assignment-2',
        customerId: 'user-2',
        assignmentType: RfidTagAssignmentType.Temporary,
        status: RfidTagAssignmentStatus.WaitingForAssignment,
        rfidTagId: null,
        assignmentDate: null,
      };
      const activeUser = { customerId: 'user-3', firstName: 'Otto', lastName: 'M', entryTime: '2026-01-03T09:00:00Z' };

      const items = toUserListItems([max, erika, otto], [assignment, waiting], [activeUser]);

      expect(items.map(({ rfidTag, inWorkshop }) => ({ rfidTag, inWorkshop }))).toEqual([
        { rfidTag: { assignmentType: RfidTagAssignmentType.Fixed, rfidTagId: '04A31F9C' }, inWorkshop: false },
        { rfidTag: { assignmentType: RfidTagAssignmentType.Temporary, rfidTagId: null }, inWorkshop: false },
        { rfidTag: null, inWorkshop: true },
      ]);
      expect(items[0]).toMatchObject(toUser(max));
    });
  });

  describe('toCreateCustomerRequest', () => {
    it('should send the date of birth as ISO date and missing optional values as null', () => {
      expect(
        toCreateCustomerRequest({
          firstName: 'Erika',
          lastName: 'Musterfrau',
          dateOfBirth: new Date(1990, 0, 31),
          clubMember: true,
          email: 'erika@example.org',
          addressLine1: 'Hauptstraße 1',
          postalCode: '12345',
          city: 'Berlin',
          country: 'Deutschland',
        }),
      ).toEqual({
        firstName: 'Erika',
        lastName: 'Musterfrau',
        dateOfBirth: '1990-01-31',
        clubMember: true,
        email: 'erika@example.org',
        phone: null,
        addressLine1: 'Hauptstraße 1',
        addressLine2: null,
        postalCode: '12345',
        city: 'Berlin',
        country: 'Deutschland',
      });
    });
  });

  // one command for the edit dialog – each request takes its part of it
  const update: UpdateUserCommand = {
    userId: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1985, 2, 15),
    clubMember: false,
    email: 'max@example.org',
    addressLine1: 'Hauptstraße 1',
    postalCode: '12345',
    city: 'Berlin',
    country: 'Deutschland',
  };

  describe('toUpdateCustomerRequest', () => {
    it('should send only the person, the date of birth as ISO date and no id', () => {
      expect(toUpdateCustomerRequest(update)).toEqual({
        firstName: 'Max',
        lastName: 'Mustermann',
        dateOfBirth: '1985-03-15',
        clubMember: false,
      });
    });
  });

  describe('toUpdateCustomerContactRequest', () => {
    it('should send only the contact, a missing phone as null and no id', () => {
      expect(toUpdateCustomerContactRequest(update)).toEqual({ email: 'max@example.org', phone: null });
    });
  });

  describe('toUpdateCustomerAddressRequest', () => {
    it('should send only the address, a missing second address line as null and no id', () => {
      expect(toUpdateCustomerAddressRequest(update)).toEqual({
        addressLine1: 'Hauptstraße 1',
        addressLine2: null,
        postalCode: '12345',
        city: 'Berlin',
        country: 'Deutschland',
      });
    });
  });
});
