import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { DeleteRfidTagCommand, RfidTag } from './rfid-tag.model';

const RFID_TAGS_API_URL = `${environment.apiBaseUrl}/api/v1/rfid-tags`;

@Service()
export class RfidTagService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads all RFID tags; reload with `reload()`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  rfidTagsResource(): HttpResourceRef<RfidTag[] | undefined> {
    return httpResource<RfidTag[]>(() => RFID_TAGS_API_URL);
  }

  // --------------------------------------------------------------------------
  // commands

  deleteRfidTag(command: DeleteRfidTagCommand): Observable<void> {
    return this.http.delete<void>(`${RFID_TAGS_API_URL}/${command.id}`);
  }
}
