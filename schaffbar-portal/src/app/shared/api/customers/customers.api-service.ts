import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';
import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import {
  CreateCustomerRequest,
  CustomerApiModel,
  customerSchema,
  UpdateCustomerAddressRequest,
  UpdateCustomerContactRequest,
  UpdateCustomerRequest,
} from './customer.api-model';

const CUSTOMERS_API_URL = `${API_URL}/customers`;

@Service()
export class CustomersApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  customersResource(): HttpResourceRef<CustomerApiModel[] | undefined> {
    return httpResource(() => CUSTOMERS_API_URL, { parse: parseWith(z.array(customerSchema), 'GET /customers') });
  }

  customerResource(id: () => string | null): HttpResourceRef<CustomerApiModel | undefined> {
    return httpResource(
      () => {
        const customerId = id();
        return customerId ? `${CUSTOMERS_API_URL}/${customerId}` : undefined;
      },
      { parse: parseWith(customerSchema, 'GET /customers/{customerId}') },
    );
  }

  // --------------------------------------------------------------------------
  // commands

  createCustomer(request: CreateCustomerRequest): Observable<void> {
    return this.http.post<void>(CUSTOMERS_API_URL, request);
  }

  updateCustomer(customerId: string, request: UpdateCustomerRequest): Observable<void> {
    return this.http.put<void>(`${CUSTOMERS_API_URL}/${customerId}`, request);
  }

  updateCustomerContact(customerId: string, request: UpdateCustomerContactRequest): Observable<void> {
    return this.http.put<void>(`${CUSTOMERS_API_URL}/${customerId}/contact`, request);
  }

  updateCustomerAddress(customerId: string, request: UpdateCustomerAddressRequest): Observable<void> {
    return this.http.put<void>(`${CUSTOMERS_API_URL}/${customerId}/address`, request);
  }

  deleteCustomer(customerId: string): Observable<void> {
    return this.http.delete<void>(`${CUSTOMERS_API_URL}/${customerId}`);
  }
}
