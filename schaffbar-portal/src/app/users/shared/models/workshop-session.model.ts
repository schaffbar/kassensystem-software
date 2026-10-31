export interface WorkshopSession {
  id: string;
  customerId: string;
  startTime: string;
  closeTime: string | null;
  status: WorkshopSessionStatus;
  workshopUsages: WorkshopUsage[];
  totalWorkshopDurationInMinutes: number;
  totalWorkshopUnits: number;
  toolUsageSummaries: ToolUsageSummary[];
}

export interface WorkshopUsage {
  id: string;
  entryTime: string;
  exitTime: string | null;
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
  startTime: string;
  endTime: string | null;
  durationInMinutes: number | null;
  unitsUsed: number | null;
}

export enum WorkshopSessionStatus {
  Open = 'OPEN',
  Paid = 'PAID',
}

// ----------------------------------------------------------------------------
// commands

export interface CloseWorkshopSessionCommand {
  customerId: string;
}
