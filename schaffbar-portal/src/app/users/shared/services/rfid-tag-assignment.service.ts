import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AssignRfidTagCommand, RfidTagAssignment, UnassignRfidTagCommand } from '../models/rfid-tag-assignment.model';

const RFID_TAG_ASSIGNMENT_API_URL = `${environment.apiBaseUrl}/api/v1/rfid-tag-assignments`;

@Service()
export class RfidTagAssignmentService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads the current RFID tag assignment of a customer (`null` if there is none) whenever the id changes; no request while the id is `null`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  rfidTagAssignmentResource(customerId: () => string | null): HttpResourceRef<RfidTagAssignment | null | undefined> {
    return httpResource(
      () => {
        const id = customerId();
        return id ? { url: RFID_TAG_ASSIGNMENT_API_URL, params: { 'customer-id': id } } : undefined;
      },
      // the backend returns a list; the customer has at most one current assignment
      { parse: (assignments) => (assignments as RfidTagAssignment[])[0] ?? null },
    );
  }

  // --------------------------------------------------------------------------
  // commands

  assignRfidTag(command: AssignRfidTagCommand): Observable<void> {
    return this.http.post<void>(RFID_TAG_ASSIGNMENT_API_URL, command);
  }

  unassignRfidTag(command: UnassignRfidTagCommand): Observable<void> {
    return this.http.put<void>(`${RFID_TAG_ASSIGNMENT_API_URL}/unassign`, command);
  }
}
