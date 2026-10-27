import { HttpClient, HttpHeaders, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { DeleteRfidTagCommand, RfidTag } from './rfid-tag.model';

const RFID_TAGS_API_URL = `${environment.apiBaseUrl}/api/v1/rfid-tags`;

@Injectable({ providedIn: 'root' })
export class RfidTagService {
  readonly http = inject(HttpClient);

  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json', //
      // Authorization: 'Bearer my-bearer', //
    }),
  };

  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads all RFID tags; reload with `reload()`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  rfidTagsResource(): HttpResourceRef<RfidTag[] | undefined> {
    return httpResource<RfidTag[]>(() => RFID_TAGS_API_URL);
  }

  getRfidTag(id: string): Observable<RfidTag> {
    return this.http.get<RfidTag>(`${RFID_TAGS_API_URL}/${id}`);
  }

  // --------------------------------------------------------------------------
  // commands

  deleteRfidTag(command: DeleteRfidTagCommand): Observable<void> {
    return this.http.delete<void>(`${RFID_TAGS_API_URL}/${command.id}`);
  }
}
