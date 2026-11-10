import { ToolCertificationStatus } from '../shared/models/tool-certification-status';

export interface ToolCertification {
  id: string;
  customerId: string;
  toolId: string;
  status: ToolCertificationStatus;
  certifiedAt: Date;
  certifiedBy: string | null;
}

export function canPauseToolCertification(status: ToolCertificationStatus): boolean {
  return status === ToolCertificationStatus.Active;
}

export function canReactivateToolCertification(status: ToolCertificationStatus): boolean {
  return status === ToolCertificationStatus.Paused;
}

export function canRevokeToolCertification(status: ToolCertificationStatus): boolean {
  return status !== ToolCertificationStatus.Revoked;
}

// ----------------------------------------------------------------------------
// commands

export interface PauseToolCertificationCommand {
  userId: string;
  toolId: string;
}

export interface ReactivateToolCertificationCommand {
  userId: string;
  toolId: string;
}

export interface RevokeToolCertificationCommand {
  userId: string;
  toolId: string;
}

export interface DeleteToolCertificationCommand {
  userId: string;
  toolId: string;
}
