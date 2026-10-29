export interface ToolCertification {
  id: string;
  customerId: string;
  toolId: string;
  status: ToolCertificationStatus;
  certifiedAt: string;
  certifiedBy: string | null;
}

export enum ToolCertificationStatus {
  Active = 'ACTIVE',
  Paused = 'PAUSED',
  Revoked = 'REVOKED',
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
