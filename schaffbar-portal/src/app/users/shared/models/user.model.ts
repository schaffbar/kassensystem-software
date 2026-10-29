export interface User {
  id: string;
  firstName: string;
  lastName: string;
  // ISO date without time (`1990-01-31`) – convert with `parseISO`
  dateOfBirth: string;
  ageGroup: AgeGroup;
  clubMember: boolean;
  email: string;
  phone: string | null;
  address: UserAddress;
  createdAt: string;
  updatedAt: string;
}

export interface UserAddress {
  addressLine1: string;
  addressLine2: string | null;
  postalCode: string;
  city: string;
  country: string;
}

export enum AgeGroup {
  Under16 = 'UNDER_16',
  Under18 = 'UNDER_18',
  Adult = 'ADULT',
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
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  clubMember: boolean;
}

export interface UpdateUserContactCommand {
  id: string;
  email: string;
  phone: string;
}

export interface UpdateUserAddressCommand {
  id: string;
  addressLine1: string;
  addressLine2: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface DeleteUserCommand {
  id: string;
}
