import { CertificationRequirement } from '../shared/models/certification-requirement';
import { ToolArea } from '../shared/models/tool-area';
import { WlanRelaisType } from '../shared/models/wlan-relais-type';

export interface Tool {
  id: string;
  name: string;
  description: string | null;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
  rfidReaderId: string | null;
  ipAddress: string | null;
  wlanRelaisType: WlanRelaisType | null;
  httpStartCommand: string | null;
  onCommand: string | null;
  offCommand: string | null;
  instructors: string[];
}

// ----------------------------------------------------------------------------
// list

export interface ToolListItem extends Tool {
  // the person who switched the tool on at the moment; null while it is free
  usedBy: string | null;
}

// ----------------------------------------------------------------------------
// WLAN relais

export interface WlanRelaisCommands {
  httpStartCommand: string;
  onCommand: string;
  offCommand: string;
}

const WLAN_RELAIS_TEMPLATES: Record<WlanRelaisType, WlanRelaisCommands> = {
  [WlanRelaisType.Shelly1]: {
    httpStartCommand: 'http://{{ipAddress}}/relay/0?',
    onCommand: 'turn=on',
    offCommand: 'turn=off',
  },
  [WlanRelaisType.Shelly2]: {
    httpStartCommand: 'http://{{ipAddress}}/relay/0?',
    onCommand: 'turn=on',
    offCommand: 'turn=off',
  },
  [WlanRelaisType.ShellyPro]: {
    httpStartCommand: 'http://{{ipAddress}}/rpc/Switch.Set?',
    onCommand: 'id=0&on=true',
    offCommand: 'id=0&on=false',
  },
};

export function wlanRelaisCommands(type: WlanRelaisType, ipAddress: string): WlanRelaisCommands {
  const template = WLAN_RELAIS_TEMPLATES[type];
  return { ...template, httpStartCommand: template.httpStartCommand.replace('{{ipAddress}}', ipAddress) };
}

// ----------------------------------------------------------------------------
// commands

export interface CreateToolCommand {
  name: string;
  description?: string;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

export interface UpdateToolCommand {
  toolId: string;
  name: string;
  description?: string;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

export interface AddToolInstructorsCommand {
  toolId: string;
  instructorIds: string[];
}

export interface RemoveToolInstructorsCommand {
  toolId: string;
  instructorIds: string[];
}

export interface ChangeRfidReaderCommand {
  toolId: string;
  rfidReaderId: string;
}

export interface ClearRfidReaderCommand {
  toolId: string;
}

export interface SetWlanRelaisCommand {
  toolId: string;
  wlanRelaisType: WlanRelaisType;
  ipAddress: string;
}

export interface ClearWlanRelaisCommand {
  toolId: string;
}

export interface DeleteToolCommand {
  toolId: string;
}
