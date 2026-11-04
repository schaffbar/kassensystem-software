import * as z from 'zod';

import { RfidReaderType } from '../../models/rfid-reader-type';

export const rfidReaderSchema = z.object({
  id: z.string(),
  macAddress: z.string(),
  type: z.enum(RfidReaderType).nullable(),
  name: z.string().nullable(),
  socketName: z.string().nullable(),
  createdAt: z.iso.datetime(),
});

export type RfidReaderApiModel = z.infer<typeof rfidReaderSchema>;

// ----------------------------------------------------------------------------
// requests

export interface UpdateRfidReaderRequest {
  name: string | null;
  socketName: string | null;
}

export interface ChangeRfidReaderTypeRequest {
  type: RfidReaderType;
}
