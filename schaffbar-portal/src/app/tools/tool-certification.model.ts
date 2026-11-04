import { ToolCertificationStatus } from '../shared/models/tool-certification-status';

export interface ToolCertification {
  id: string;
  customerId: string;
  toolId: string;
  status: ToolCertificationStatus;
  certifiedAt: Date;
  certifiedBy: string | null;
}

// ----------------------------------------------------------------------------
// commands

export interface BatchCreateToolCertificationCommand {
  customerIds: string[];
  toolId: string;
  certifiedBy: string;
}

export interface DeleteToolCertificationCommand {
  customerId: string;
  toolId: string;
}
