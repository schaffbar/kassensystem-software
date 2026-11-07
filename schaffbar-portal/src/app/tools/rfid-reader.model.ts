import { RfidReaderType } from '../shared/models/rfid-reader-type';

export interface RfidReader {
  id: string;
  macAddress: string;
  name: string | null;
  type: RfidReaderType | null;
}
