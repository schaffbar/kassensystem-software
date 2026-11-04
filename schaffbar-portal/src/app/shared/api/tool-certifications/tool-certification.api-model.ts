import * as z from 'zod';

import { ToolCertificationStatus } from '../../models/tool-certification-status';

export const toolCertificationSchema = z.object({
  id: z.string(),
  customerId: z.string(),
  toolId: z.string(),
  status: z.enum(ToolCertificationStatus),
  certifiedAt: z.iso.datetime(),
  certifiedBy: z.string().nullable(),
});

export type ToolCertificationApiModel = z.infer<typeof toolCertificationSchema>;

// ----------------------------------------------------------------------------
// requests

export interface BatchCertifyRequest {
  customerIds: string[];
  toolId: string;
  certifiedBy: string;
}
