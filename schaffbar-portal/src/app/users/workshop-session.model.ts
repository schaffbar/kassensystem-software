import { WorkshopSessionStatus } from '../shared/models/workshop-session-status';

export interface WorkshopSession {
  id: string;
  customerId: string;
  startTime: Date;
  closeTime: Date | null;
  status: WorkshopSessionStatus;
  workshopUsages: WorkshopUsage[];
  totalWorkshopDurationInMinutes: number;
  totalWorkshopUnits: number;
  toolUsageSummaries: ToolUsageSummary[];
}

export interface WorkshopUsage {
  id: string;
  entryTime: Date;
  exitTime: Date | null;
  durationInMinutes: number | null;
  unitsUsed: number | null;
}

export interface ToolUsageSummary {
  toolId: string;
  toolName: string | null; // null if the tool has been deleted
  active: boolean;
  totalDurationInMinutes: number;
  totalUnits: number;
  usages: ToolUsage[];
}

export interface ToolUsage {
  id: string;
  startTime: Date;
  endTime: Date | null;
  durationInMinutes: number | null;
  unitsUsed: number | null;
}

export function hasRunningUsage(session: WorkshopSession): boolean {
  return (
    session.workshopUsages.some((usage) => usage.exitTime === null) ||
    session.toolUsageSummaries.some((summary) => summary.active)
  );
}

// ----------------------------------------------------------------------------
// commands

export interface CloseWorkshopSessionCommand {
  customerId: string;
}
