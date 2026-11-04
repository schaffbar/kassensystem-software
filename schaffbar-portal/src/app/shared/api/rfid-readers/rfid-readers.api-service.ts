import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';
import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import {
  ChangeRfidReaderTypeRequest,
  RfidReaderApiModel,
  rfidReaderSchema,
  UpdateRfidReaderRequest,
} from './rfid-reader.api-model';

const RFID_READERS_API_URL = `${API_URL}/rfid-readers`;

@Service()
export class RfidReadersApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads all RFID readers; reload with `reload()`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  rfidReadersResource(): HttpResourceRef<RfidReaderApiModel[] | undefined> {
    return httpResource(() => RFID_READERS_API_URL, {
      parse: parseWith(z.array(rfidReaderSchema), 'GET /rfid-readers'),
    });
  }

  /**
   * Loads the RFID reader whenever the id changes; no request while the id is `null`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  rfidReaderResource(id: () => string | null): HttpResourceRef<RfidReaderApiModel | undefined> {
    return httpResource(
      () => {
        const rfidReaderId = id();
        return rfidReaderId ? `${RFID_READERS_API_URL}/${rfidReaderId}` : undefined;
      },
      { parse: parseWith(rfidReaderSchema, 'GET /rfid-readers/{rfidReaderId}') },
    );
  }

  // --------------------------------------------------------------------------
  // commands

  updateRfidReader(rfidReaderId: string, request: UpdateRfidReaderRequest): Observable<void> {
    return this.http.put<void>(`${RFID_READERS_API_URL}/${rfidReaderId}`, request);
  }

  changeRfidReaderType(rfidReaderId: string, request: ChangeRfidReaderTypeRequest): Observable<void> {
    return this.http.patch<void>(`${RFID_READERS_API_URL}/${rfidReaderId}/type`, request);
  }

  deleteRfidReader(rfidReaderId: string): Observable<void> {
    return this.http.delete<void>(`${RFID_READERS_API_URL}/${rfidReaderId}`);
  }
}
