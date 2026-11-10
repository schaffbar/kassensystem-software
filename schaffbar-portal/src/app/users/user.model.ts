import { isSameDay } from 'date-fns';

import { AgeGroup } from '../shared/models/age-group';
import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  ageGroup: AgeGroup;
  clubMember: boolean;
  email: string;
  phone: string | null;
  address: UserAddress;
  createdAt: Date;
  updatedAt: Date;
}

export interface UserAddress {
  addressLine1: string;
  addressLine2: string | null;
  postalCode: string;
  city: string;
  country: string;
}

// ----------------------------------------------------------------------------
// list

export interface UserListItem extends User {
  // null = no tag and none requested
  rfidTag: UserRfidTag | null;
  // entered the workshop and has not left yet
  inWorkshop: boolean;
}

export interface UserRfidTag {
  assignmentType: RfidTagAssignmentType;
  // null while the user waits for a tag at the counter
  rfidTagId: string | null;
}

// a loan tag goes back at the counter when leaving – who still has one at the end of the day has not returned it
export function hasTemporaryRfidTag(user: UserListItem): boolean {
  return !!user.rfidTag?.rfidTagId && user.rfidTag.assignmentType === RfidTagAssignmentType.Temporary;
}

// ----------------------------------------------------------------------------
// update – only the parts that changed are sent

export function hasPersonChanged(user: User, command: UpdateUserCommand): boolean {
  return (
    user.firstName !== command.firstName ||
    user.lastName !== command.lastName ||
    !isSameDay(user.dateOfBirth, command.dateOfBirth) ||
    user.clubMember !== command.clubMember
  );
}

export function hasContactChanged(user: User, command: UpdateUserCommand): boolean {
  return user.email !== command.email || (user.phone ?? undefined) !== command.phone;
}

export function hasAddressChanged(user: User, command: UpdateUserCommand): boolean {
  const { address } = user;
  return (
    address.addressLine1 !== command.addressLine1 ||
    (address.addressLine2 ?? undefined) !== command.addressLine2 ||
    address.postalCode !== command.postalCode ||
    address.city !== command.city ||
    address.country !== command.country
  );
}

// ----------------------------------------------------------------------------
// commands

export interface CreateUserCommand {
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  clubMember: boolean;
  email: string;
  phone?: string;
  addressLine1: string;
  addressLine2?: string;
  postalCode: string;
  city: string;
  country: string;
}

// person, contact and address in one dialog – the backend saves them with three endpoints (see hasPersonChanged …)
export interface UpdateUserCommand {
  userId: string;
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  clubMember: boolean;
  email: string;
  phone?: string;
  addressLine1: string;
  addressLine2?: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface DeleteUserCommand {
  userId: string;
}
