import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CloseWorkshopSessionCommand, WorkshopSession } from '../models/workshop-session.model';

const WORKSHOP_SESSION_API_URL = `${environment.apiBaseUrl}/api/v1/workshop-sessions`;

@Service()
export class WorkshopSessionService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads the open workshop session of a customer whenever the id changes; no request while the id is `null`.
   * `null` if there is no open session – the backend answers 200 with an empty body.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  openSessionResource(customerId: () => string | null): HttpResourceRef<WorkshopSession | null | undefined> {
    return httpResource<WorkshopSession | null>(() => {
      const id = customerId();
      return id ? `${WORKSHOP_SESSION_API_URL}/${id}/active` : undefined;
    });
  }

  // --------------------------------------------------------------------------
  // commands

  closeWorkshopSession(command: CloseWorkshopSessionCommand): Observable<void> {
    return this.http.put<void>(`${WORKSHOP_SESSION_API_URL}/${command.customerId}/close`, {});
  }
}
