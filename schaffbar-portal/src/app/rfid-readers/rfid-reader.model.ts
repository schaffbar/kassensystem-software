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
  rfidReaderId: string;
  name?: string;
  socketName?: string;
}

export interface ChangeRfidReaderTypeCommand {
  rfidReaderId: string;
  type: RfidReaderType;
}

export interface DeleteRfidReaderCommand {
  rfidReaderId: string;
}
