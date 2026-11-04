import { RfidReaderType } from '../shared/models/rfid-reader-type';

export interface RfidReader {
  id: string;
  macAddress: string;
  type: RfidReaderType | null;
  name: string | null;
  socketName: string | null;
  createdAt: Date;
}

// ----------------------------------------------------------------------------
// commands

export interface UpdateRfidReaderCommand {
  id: string;
  name?: string;
  socketName?: string;
}

export interface ChangeRfidReaderTypeCommand {
  id: string;
  type: RfidReaderType;
}

export interface DeleteRfidReaderCommand {
  id: string;
}
