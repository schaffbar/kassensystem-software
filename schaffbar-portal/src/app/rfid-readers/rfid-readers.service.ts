import { HttpClient, HttpHeaders, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  ChangeRfidReaderTypeCommand,
  DeleteRfidReaderCommand,
  RfidReader,
  UpdateRfidReaderCommand,
} from './rfid-reader.model';

const RFID_READERS_API_URL = `${environment.apiBaseUrl}/api/v1/rfid-readers`;

@Injectable({ providedIn: 'root' })
export class RfidReaderService {
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
   * Loads all RFID readers; reload with `reload()`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  rfidReadersResource(): HttpResourceRef<RfidReader[] | undefined> {
    return httpResource<RfidReader[]>(() => RFID_READERS_API_URL);
  }

  /**
   * Loads the RFID reader whenever the id changes; no request while the id is `null`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  rfidReaderResource(id: () => string | null): HttpResourceRef<RfidReader | undefined> {
    return httpResource<RfidReader>(() => {
      const rfidReaderId = id();
      return rfidReaderId ? `${RFID_READERS_API_URL}/${rfidReaderId}` : undefined;
    });
  }

  // --------------------------------------------------------------------------
  // commands

  updateRfidReader(command: UpdateRfidReaderCommand): Observable<void> {
    return this.http.put<void>(`${RFID_READERS_API_URL}/${command.id}`, command, this.httpOptions);
  }

  changeRfidReaderType(command: ChangeRfidReaderTypeCommand): Observable<void> {
    return this.http.patch<void>(`${RFID_READERS_API_URL}/${command.id}/type`, command, this.httpOptions);
  }

  deleteRfidReader(command: DeleteRfidReaderCommand): Observable<void> {
    return this.http.delete<void>(`${RFID_READERS_API_URL}/${command.id}`);
  }
}
