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

export interface UpdateUserCommand {
  userId: string;
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  clubMember: boolean;
}

export interface UpdateUserContactCommand {
  userId: string;
  email: string;
  phone?: string;
}

export interface UpdateUserAddressCommand {
  userId: string;
  addressLine1: string;
  addressLine2?: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface DeleteUserCommand {
  userId: string;
}
