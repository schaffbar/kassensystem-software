import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import { WorkshopSessionApiModel, workshopSessionSchema } from './workshop-session.api-model';

const WORKSHOP_SESSIONS_API_URL = `${API_URL}/workshop-sessions`;

@Service()
export class WorkshopSessionsApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  openSessionResource(customerId: () => string | null): HttpResourceRef<WorkshopSessionApiModel | null | undefined> {
    return httpResource(
      () => {
        const id = customerId();
        return id ? `${WORKSHOP_SESSIONS_API_URL}/${id}/active` : undefined;
      },
      { parse: parseWith(workshopSessionSchema.nullable(), 'GET /workshop-sessions/{customerId}/active') },
    );
  }

  // --------------------------------------------------------------------------
  // commands

  closeWorkshopSession(customerId: string): Observable<void> {
    return this.http.put<void>(`${WORKSHOP_SESSIONS_API_URL}/${customerId}/close`, {});
  }
}
