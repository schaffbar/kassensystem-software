import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  BatchCreateToolCertificationCommand,
  DeleteToolCertificationCommand,
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
  ToolCertification,
} from '../models/tool-certification.model';

const TOOL_CERTIFICATIONS_API_URL = `${environment.apiBaseUrl}/api/v1/tool-certifications`;

@Service()
export class ToolCertificationService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads the certifications of a customer whenever the id changes; no request while the id is `null`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  certificationsByUserResource(customerId: () => string | null): HttpResourceRef<ToolCertification[] | undefined> {
    return httpResource<ToolCertification[]>(() => {
      const id = customerId();
      return id ? { url: TOOL_CERTIFICATIONS_API_URL, params: { customerId: id } } : undefined;
    });
  }

  /**
   * Loads the certifications of a tool whenever the tool id changes; no request while the id is `null`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  certificationsByToolResource(toolId: () => string | null): HttpResourceRef<ToolCertification[] | undefined> {
    return httpResource<ToolCertification[]>(() => {
      const id = toolId();
      return id ? { url: TOOL_CERTIFICATIONS_API_URL, params: { toolId: id } } : undefined;
    });
  }

  // --------------------------------------------------------------------------
  // commands

  batchCreate(command: BatchCreateToolCertificationCommand): Observable<void> {
    return this.http.post<void>(`${TOOL_CERTIFICATIONS_API_URL}/batch`, command);
  }

  pause(command: PauseToolCertificationCommand): Observable<void> {
    return this.http.put<void>(
      `${TOOL_CERTIFICATIONS_API_URL}/customers/${command.customerId}/tools/${command.toolId}/pause`,
      {},
    );
  }

  reactivate(command: ReactivateToolCertificationCommand): Observable<void> {
    return this.http.put<void>(
      `${TOOL_CERTIFICATIONS_API_URL}/customers/${command.customerId}/tools/${command.toolId}/reactivate`,
      {},
    );
  }

  revoke(command: RevokeToolCertificationCommand): Observable<void> {
    return this.http.put<void>(
      `${TOOL_CERTIFICATIONS_API_URL}/customers/${command.customerId}/tools/${command.toolId}/revoke`,
      {},
    );
  }

  delete(command: DeleteToolCertificationCommand): Observable<void> {
    return this.http.delete<void>(
      `${TOOL_CERTIFICATIONS_API_URL}/customers/${command.customerId}/tools/${command.toolId}`,
    );
  }
}
