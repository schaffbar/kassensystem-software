import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { map, Observable } from 'rxjs';
import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import {
  CreateToolRequest,
  InstructorsRequest,
  SetWlanRelaisRequest,
  ToolApiModel,
  toolSchema,
  UpdateToolRequest,
} from './tool.api-model';

const TOOLS_API_URL = `${API_URL}/tools`;

@Service()
export class ToolsApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  toolsResource(): HttpResourceRef<ToolApiModel[] | undefined> {
    return httpResource(() => TOOLS_API_URL, { parse: parseWith(z.array(toolSchema), 'GET /tools') });
  }

  toolResource(id: () => string | null): HttpResourceRef<ToolApiModel | undefined> {
    return httpResource(
      () => {
        const toolId = id();
        return toolId ? `${TOOLS_API_URL}/${toolId}` : undefined;
      },
      { parse: parseWith(toolSchema, 'GET /tools/{toolId}') },
    );
  }

  getToolsByRfidReader(rfidReaderId: string): Observable<ToolApiModel[]> {
    return this.http
      .get<unknown>(TOOLS_API_URL, { params: { rfidReaderId } })
      .pipe(map(parseWith(z.array(toolSchema), 'GET /tools?rfidReaderId')));
  }

  // --------------------------------------------------------------------------
  // commands

  createTool(request: CreateToolRequest): Observable<void> {
    return this.http.post<void>(TOOLS_API_URL, request);
  }

  updateTool(toolId: string, request: UpdateToolRequest): Observable<void> {
    return this.http.put<void>(`${TOOLS_API_URL}/${toolId}`, request);
  }

  assignRfidReader(toolId: string, rfidReaderId: string): Observable<void> {
    return this.http.put<void>(`${TOOLS_API_URL}/${toolId}/rfid-reader/${rfidReaderId}`, {});
  }

  clearRfidReader(toolId: string): Observable<void> {
    return this.http.delete<void>(`${TOOLS_API_URL}/${toolId}/rfid-reader`);
  }

  setWlanRelais(toolId: string, request: SetWlanRelaisRequest): Observable<void> {
    return this.http.put<void>(`${TOOLS_API_URL}/${toolId}/wlan-relais`, request);
  }

  clearWlanRelais(toolId: string): Observable<void> {
    return this.http.delete<void>(`${TOOLS_API_URL}/${toolId}/wlan-relais`);
  }

  addInstructors(toolId: string, request: InstructorsRequest): Observable<void> {
    return this.http.post<void>(`${TOOLS_API_URL}/${toolId}/instructors`, request);
  }

  removeInstructors(toolId: string, request: InstructorsRequest): Observable<void> {
    return this.http.delete<void>(`${TOOLS_API_URL}/${toolId}/instructors`, { body: request });
  }

  deleteTool(toolId: string): Observable<void> {
    return this.http.delete<void>(`${TOOLS_API_URL}/${toolId}`);
  }
}
