import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';
import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import {
  BatchCertifyRequest,
  ToolCertificationApiModel,
  toolCertificationSchema,
} from './tool-certification.api-model';

const TOOL_CERTIFICATIONS_API_URL = `${API_URL}/tool-certifications`;

@Service()
export class ToolCertificationsApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  toolCertificationsByCustomerResource(
    customerId: () => string | null,
  ): HttpResourceRef<ToolCertificationApiModel[] | undefined> {
    return httpResource(
      () => {
        const id = customerId();
        return id ? { url: TOOL_CERTIFICATIONS_API_URL, params: { customerId: id } } : undefined;
      },
      { parse: parseWith(z.array(toolCertificationSchema), 'GET /tool-certifications?customerId') },
    );
  }

  toolCertificationsByToolResource(
    toolId: () => string | null,
  ): HttpResourceRef<ToolCertificationApiModel[] | undefined> {
    return httpResource(
      () => {
        const id = toolId();
        return id ? { url: TOOL_CERTIFICATIONS_API_URL, params: { toolId: id } } : undefined;
      },
      { parse: parseWith(z.array(toolCertificationSchema), 'GET /tool-certifications?toolId') },
    );
  }

  // --------------------------------------------------------------------------
  // commands

  batchCertify(request: BatchCertifyRequest): Observable<void> {
    return this.http.post<void>(`${TOOL_CERTIFICATIONS_API_URL}/batch`, request);
  }

  pauseToolCertification(customerId: string, toolId: string): Observable<void> {
    return this.http.put<void>(`${certificationUrl(customerId, toolId)}/pause`, {});
  }

  reactivateToolCertification(customerId: string, toolId: string): Observable<void> {
    return this.http.put<void>(`${certificationUrl(customerId, toolId)}/reactivate`, {});
  }

  revokeToolCertification(customerId: string, toolId: string): Observable<void> {
    return this.http.put<void>(`${certificationUrl(customerId, toolId)}/revoke`, {});
  }

  deleteToolCertification(customerId: string, toolId: string): Observable<void> {
    return this.http.delete<void>(certificationUrl(customerId, toolId));
  }
}

function certificationUrl(customerId: string, toolId: string): string {
  return `${TOOL_CERTIFICATIONS_API_URL}/customers/${customerId}/tools/${toolId}`;
}
