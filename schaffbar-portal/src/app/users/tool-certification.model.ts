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

export interface PauseToolCertificationCommand {
  customerId: string;
  toolId: string;
}

export interface ReactivateToolCertificationCommand {
  customerId: string;
  toolId: string;
}

export interface RevokeToolCertificationCommand {
  customerId: string;
  toolId: string;
}
