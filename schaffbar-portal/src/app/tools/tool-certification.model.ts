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

export interface BatchCreateToolCertificationsCommand {
  userIds: string[];
  toolId: string;
  certifiedBy: string;
}

export interface DeleteToolCertificationCommand {
  userId: string;
  toolId: string;
}
