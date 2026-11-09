import { format, parseISO } from 'date-fns';

import {
  CreateCustomerRequest,
  CustomerAddressApiModel,
  CustomerApiModel,
  UpdateCustomerAddressRequest,
  UpdateCustomerContactRequest,
  UpdateCustomerRequest,
} from '../shared/api/customers/customer.api-model';
import { RfidTagAssignmentApiModel } from '../shared/api/rfid-tag-assignments/rfid-tag-assignment.api-model';
import { WorkshopDashboardEntryApiModel } from '../shared/api/workshop-dashboard/workshop-dashboard.api-model';
import {
  CreateUserCommand,
  UpdateUserAddressCommand,
  UpdateUserCommand,
  UpdateUserContactCommand,
  User,
  UserAddress,
  UserListItem,
  UserRfidTag,
} from './user.model';

const ISO_DATE = 'yyyy-MM-dd';

// ----------------------------------------------------------------------------
// API model → feature model

export function toUser(apiModel: CustomerApiModel): User {
  return {
    id: apiModel.id,
    firstName: apiModel.firstName,
    lastName: apiModel.lastName,
    // parseISO creates a local date (new Date('1990-01-31') would be midnight UTC)
    dateOfBirth: parseISO(apiModel.dateOfBirth),
    ageGroup: apiModel.ageGroup,
    clubMember: apiModel.clubMember,
    email: apiModel.email,
    phone: apiModel.phone,
    address: toUserAddress(apiModel.address),
    createdAt: parseISO(apiModel.createdAt),
    updatedAt: parseISO(apiModel.updatedAt),
  };
}

function toUserAddress(apiModel: CustomerAddressApiModel): UserAddress {
  return {
    addressLine1: apiModel.addressLine1,
    addressLine2: apiModel.addressLine2,
    postalCode: apiModel.postalCode,
    city: apiModel.city,
    country: apiModel.country,
  };
}

// the tag comes from the user's assignment, the presence from the workshop dashboard
export function toUserListItems(
  customers: CustomerApiModel[],
  assignments: RfidTagAssignmentApiModel[],
  activeUsers: WorkshopDashboardEntryApiModel[],
): UserListItem[] {
  const assignmentsByCustomerId = new Map(assignments.map((assignment) => [assignment.customerId, assignment]));
  const activeCustomerIds = new Set(activeUsers.map((activeUser) => activeUser.customerId));
  return customers.map((customer) => {
    const assignment = assignmentsByCustomerId.get(customer.id);
    return {
      ...toUser(customer),
      rfidTag: assignment ? toUserRfidTag(assignment) : null,
      inWorkshop: activeCustomerIds.has(customer.id),
    };
  });
}

function toUserRfidTag(assignment: RfidTagAssignmentApiModel): UserRfidTag {
  return { assignmentType: assignment.assignmentType, rfidTagId: assignment.rfidTagId };
}

// ----------------------------------------------------------------------------
// command → request

export function toCreateCustomerRequest(command: CreateUserCommand): CreateCustomerRequest {
  return {
    firstName: command.firstName,
    lastName: command.lastName,
    dateOfBirth: format(command.dateOfBirth, ISO_DATE),
    clubMember: command.clubMember,
    email: command.email,
    phone: command.phone ?? null,
    addressLine1: command.addressLine1,
    addressLine2: command.addressLine2 ?? null,
    postalCode: command.postalCode,
    city: command.city,
    country: command.country,
  };
}

export function toUpdateCustomerRequest(command: UpdateUserCommand): UpdateCustomerRequest {
  return {
    firstName: command.firstName,
    lastName: command.lastName,
    dateOfBirth: format(command.dateOfBirth, ISO_DATE),
    clubMember: command.clubMember,
  };
}

export function toUpdateCustomerContactRequest(command: UpdateUserContactCommand): UpdateCustomerContactRequest {
  return {
    email: command.email,
    phone: command.phone ?? null,
  };
}

export function toUpdateCustomerAddressRequest(command: UpdateUserAddressCommand): UpdateCustomerAddressRequest {
  return {
    addressLine1: command.addressLine1,
    addressLine2: command.addressLine2 ?? null,
    postalCode: command.postalCode,
    city: command.city,
    country: command.country,
  };
}
