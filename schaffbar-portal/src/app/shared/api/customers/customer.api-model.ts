import * as z from 'zod';

import { AgeGroup } from '../../models/age-group';

export const customerAddressSchema = z.object({
  addressLine1: z.string(),
  addressLine2: z.string().nullable(),
  postalCode: z.string(),
  city: z.string(),
  country: z.string(),
});

export const customerSchema = z.object({
  id: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  dateOfBirth: z.iso.date(),
  ageGroup: z.enum(AgeGroup),
  clubMember: z.boolean(),
  email: z.string(),
  phone: z.string().nullable(),
  address: customerAddressSchema,
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime(),
});

export type CustomerAddressApiModel = z.infer<typeof customerAddressSchema>;
export type CustomerApiModel = z.infer<typeof customerSchema>;

// ----------------------------------------------------------------------------
// requests

export interface CreateCustomerRequest {
  firstName: string;
  lastName: string;
  // ISO date without time (`1990-01-31`)
  dateOfBirth: string;
  clubMember: boolean;
  email: string;
  phone: string | null;
  addressLine1: string;
  addressLine2: string | null;
  postalCode: string;
  city: string;
  country: string;
}

export interface UpdateCustomerRequest {
  firstName: string;
  lastName: string;
  // ISO date without time (`1990-01-31`)
  dateOfBirth: string;
  clubMember: boolean;
}

export interface UpdateCustomerContactRequest {
  email: string;
  phone: string | null;
}

export interface UpdateCustomerAddressRequest {
  addressLine1: string;
  addressLine2: string | null;
  postalCode: string;
  city: string;
  country: string;
}
