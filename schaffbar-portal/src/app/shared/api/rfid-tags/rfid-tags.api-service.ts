import { HttpClient, httpResource, HttpResourceRef } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { Observable } from 'rxjs';
import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import { RfidTagApiModel, rfidTagSchema } from './rfid-tag.api-model';

const RFID_TAGS_API_URL = `${API_URL}/rfid-tags`;

@Service()
export class RfidTagsApiService {
  private readonly http = inject(HttpClient);

  // --------------------------------------------------------------------------
  // queries

  rfidTagsResource(): HttpResourceRef<RfidTagApiModel[] | undefined> {
    return httpResource(() => RFID_TAGS_API_URL, {
      parse: parseWith(z.array(rfidTagSchema), 'GET /rfid-tags'),
    });
  }

  // --------------------------------------------------------------------------
  // commands

  deleteRfidTag(rfidTagId: string): Observable<void> {
    return this.http.delete<void>(`${RFID_TAGS_API_URL}/${rfidTagId}`);
  }
}
