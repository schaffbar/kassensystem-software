import { HttpClient, httpResource, HttpResourceRef } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { map, Observable } from 'rxjs';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import { WorkshopSessionApiModel, workshopSessionSchema } from './workshop-session.api-model';

const WORKSHOP_SESSIONS_API_URL = `${API_URL}/workshop-sessions`;

@Service()
export class WorkshopSessionsApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  openWorkshopSessionResource(
    customerId: () => string | null,
  ): HttpResourceRef<WorkshopSessionApiModel | null | undefined> {
    return httpResource(
      () => {
        const id = customerId();
        return id ? `${WORKSHOP_SESSIONS_API_URL}/${id}/open` : undefined;
      },
      { parse: parseWith(workshopSessionSchema.nullable(), 'GET /workshop-sessions/{customerId}/open') },
    );
  }

  getOpenWorkshopSession(customerId: string): Observable<WorkshopSessionApiModel | null> {
    return this.http
      .get<unknown>(`${WORKSHOP_SESSIONS_API_URL}/${customerId}/open`)
      .pipe(map(parseWith(workshopSessionSchema.nullable(), 'GET /workshop-sessions/{customerId}/open')));
  }

  // --------------------------------------------------------------------------
  // commands

  closeWorkshopSession(customerId: string): Observable<void> {
    return this.http.put<void>(`${WORKSHOP_SESSIONS_API_URL}/${customerId}/close`, {});
  }
}
