import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';
import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import {
  RequestRfidTagAssignmentRequest,
  RfidTagAssignmentApiModel,
  rfidTagAssignmentSchema,
  UnassignRfidTagRequest,
} from './rfid-tag-assignment.api-model';

const RFID_TAG_ASSIGNMENTS_API_URL = `${API_URL}/rfid-tag-assignments`;

@Service()
export class RfidTagAssignmentsApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  currentRfidTagAssignmentResource(
    customerId: () => string | null,
  ): HttpResourceRef<RfidTagAssignmentApiModel | null | undefined> {
    const parseAssignments = parseWith(z.array(rfidTagAssignmentSchema), 'GET /rfid-tag-assignments?customer-id');
    return httpResource(
      () => {
        const id = customerId();
        return id ? { url: RFID_TAG_ASSIGNMENTS_API_URL, params: { 'customer-id': id } } : undefined;
      },
      // the backend returns a list; the customer has at most one current assignment
      { parse: (response) => parseAssignments(response)[0] ?? null },
    );
  }

  // --------------------------------------------------------------------------
  // commands

  requestRfidTagAssignment(request: RequestRfidTagAssignmentRequest): Observable<void> {
    return this.http.post<void>(RFID_TAG_ASSIGNMENTS_API_URL, request);
  }

  unassignRfidTag(request: UnassignRfidTagRequest): Observable<void> {
    return this.http.put<void>(`${RFID_TAG_ASSIGNMENTS_API_URL}/unassign`, request);
  }
}
