import { RfidReaderType } from '../shared/models/rfid-reader-type';

export interface RfidReader {
  id: string;
  name: string | null;
  type: RfidReaderType | null;
}
