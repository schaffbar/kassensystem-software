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
// list

// the tool a switch box switches
export interface AssignedTool {
  id: string;
  name: string;
}

export interface RfidReaderListItem extends RfidReader {
  assignedTool: AssignedTool | null;
}

// where a reader appears in the list: new ones without type, at the gates and the counter, at a tool
export type RfidReaderGroup = 'new' | 'desk' | 'switchBox';

export function rfidReaderGroup(type: RfidReaderType | null): RfidReaderGroup {
  if (!type) return 'new';
  return type === RfidReaderType.SwitchBox ? 'switchBox' : 'desk';
}

// not set up yet: a reader without type does nothing, a switch box without tool switches nothing
export function isUnassigned(rfidReader: RfidReaderListItem): boolean {
  return !rfidReader.type || (rfidReader.type === RfidReaderType.SwitchBox && !rfidReader.assignedTool);
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
