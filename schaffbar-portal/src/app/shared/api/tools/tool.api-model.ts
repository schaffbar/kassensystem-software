import * as z from 'zod';

import { CertificationRequirement } from '../../models/certification-requirement';
import { ToolArea } from '../../models/tool-area';
import { WlanRelaisType } from '../../models/wlan-relais-type';

export const toolSchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string().nullable(),
  area: z.enum(ToolArea),
  certificationRequirement: z.enum(CertificationRequirement),
  rfidReaderId: z.string().nullable(),
  wlanRelaisType: z.enum(WlanRelaisType).nullable(),
  ipAddress: z.string().nullable(),
  httpStartCommand: z.string().nullable(),
  onCommand: z.string().nullable(),
  offCommand: z.string().nullable(),
  instructors: z.array(z.string()),
});

export type ToolApiModel = z.infer<typeof toolSchema>;

// ----------------------------------------------------------------------------
// requests

export interface CreateToolRequest {
  name: string;
  description: string | null;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

export interface UpdateToolRequest {
  name: string;
  description: string | null;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

export interface SetWlanRelaisRequest {
  wlanRelaisType: WlanRelaisType;
  ipAddress: string;
}

export interface InstructorsRequest {
  instructorIds: string[];
}
