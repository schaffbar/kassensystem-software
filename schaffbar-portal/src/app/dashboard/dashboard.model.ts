import { isSameDay } from 'date-fns';

import { AgeGroup } from '../shared/models/age-group';
import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { ToolArea } from '../shared/models/tool-area';
import { WlanRelaisType } from '../shared/models/wlan-relais-type';

// checked in and not checked out yet
export interface ActiveUser {
  customerId: string;
  firstName: string;
  lastName: string;
  entryTime: Date;
}

// switched on by the person
export interface ActiveTool {
  toolId: string;
  toolName: string;
  customerId: string;
  firstName: string;
  lastName: string;
}

export interface Tool {
  id: string;
  name: string;
  area: ToolArea;
  rfidReaderId: string | null;
  wlanRelaisType: WlanRelaisType | null;
}

// what the overview shows of a person besides the name
export interface User {
  id: string;
  ageGroup: AgeGroup;
  clubMember: boolean;
}

export interface RfidReader {
  id: string;
  name: string | null;
  macAddress: string;
  type: RfidReaderType | null;
}

// ----------------------------------------------------------------------------
// needs attention

// without switch box and WLAN relais the tool can't be unlocked by RFID (like `isHardwareSetUp` of the tools)
export function isHardwareSetUp(tool: Tool): boolean {
  return !!tool.rfidReaderId && !!tool.wlanRelaisType;
}

// a new reader registers itself without type – it does nothing until somebody sets it up
export function needsRfidReaderType(rfidReader: RfidReader): boolean {
  return rfidReader.type === null;
}

export function isMinor(user: User): boolean {
  return user.ageGroup !== AgeGroup.Adult;
}

// a stay never lasts over night – who checked in on an earlier day has not checked out at the exit
export function hasStayedOvernight(activeUser: ActiveUser, today: Date): boolean {
  return !isSameDay(activeUser.entryTime, today);
}
