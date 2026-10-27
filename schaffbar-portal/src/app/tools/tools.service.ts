import { HttpClient, HttpHeaders, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  AddToolInstructorsCommand,
  ChangeRfidReaderCommand,
  ClearRfidReaderCommand,
  ClearWlanRelaisCommand,
  CreateToolCommand,
  DeleteToolCommand,
  RemoveToolInstructorsCommand,
  SetWlanRelaisCommand,
  Tool,
  UpdateToolCommand,
} from './tool.model';

const TOOLS_API_URL = `${environment.apiBaseUrl}/api/v1/tools`;

@Injectable({ providedIn: 'root' })
export class ToolsService {
  readonly http = inject(HttpClient);

  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json', //
      // Authorization: 'Bearer my-bearer', //
    }),
  };

  // --------------------------------------------------------------------------
  // queries

  getTools(): Observable<Tool[]> {
    return this.http.get<Tool[]>(TOOLS_API_URL);
  }

  /**
   * Loads the tool whenever the id changes; no request while the id is `null`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  toolResource(id: () => string | null): HttpResourceRef<Tool | undefined> {
    return httpResource<Tool>(() => {
      const toolId = id();
      return toolId ? `${TOOLS_API_URL}/${toolId}` : undefined;
    });
  }

  getToolsByRfidReader(rfidReaderId: string): Observable<Tool[]> {
    return this.http.get<Tool[]>(`${TOOLS_API_URL}?rfidReaderId=${rfidReaderId}`);
  }

  // --------------------------------------------------------------------------
  // commands

  createTool(command: CreateToolCommand): Observable<Tool> {
    return this.http.post<Tool>(TOOLS_API_URL, command, this.httpOptions);
  }

  updateTool(command: UpdateToolCommand): Observable<void> {
    return this.http.put<void>(`${TOOLS_API_URL}/${command.id}`, command, this.httpOptions);
  }

  changeRfidReader(command: ChangeRfidReaderCommand): Observable<void> {
    const url = `${TOOLS_API_URL}/${command.toolId}/rfid-reader/${command.rfidReaderId}`;
    return this.http.put<void>(url, {}, this.httpOptions);
  }

  clearRfidReader(command: ClearRfidReaderCommand): Observable<void> {
    const url = `${TOOLS_API_URL}/${command.toolId}/rfid-reader`;
    return this.http.delete<void>(url);
  }

  setWlanRelais(command: SetWlanRelaisCommand): Observable<void> {
    const url = `${TOOLS_API_URL}/${command.toolId}/wlan-relais`;
    return this.http.put<void>(url, command, this.httpOptions);
  }

  clearWlanRelais(command: ClearWlanRelaisCommand): Observable<void> {
    const url = `${TOOLS_API_URL}/${command.toolId}/wlan-relais`;
    return this.http.delete<void>(url);
  }

  addInstructors(command: AddToolInstructorsCommand): Observable<void> {
    const url = `${TOOLS_API_URL}/${command.toolId}/instructors`;
    return this.http.post<void>(url, { instructorIds: command.instructorIds }, this.httpOptions);
  }

  removeInstructors(command: RemoveToolInstructorsCommand): Observable<void> {
    const url = `${TOOLS_API_URL}/${command.toolId}/instructors`;
    return this.http.delete<void>(url, { ...this.httpOptions, body: { instructorIds: command.instructorIds } });
  }

  deleteTool(command: DeleteToolCommand): Observable<void> {
    return this.http.delete<void>(`${TOOLS_API_URL}/${command.id}`);
  }
}
