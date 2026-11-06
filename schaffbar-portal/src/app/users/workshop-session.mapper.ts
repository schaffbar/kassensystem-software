import { parseISO } from 'date-fns';

import {
  ToolUsageDetailApiModel,
  ToolUsageSummaryApiModel,
  WorkshopSessionApiModel,
  WorkshopUsageApiModel,
} from '../shared/api/workshop-sessions/workshop-session.api-model';
import { ToolUsage, ToolUsageSummary, WorkshopSession, WorkshopUsage } from './workshop-session.model';

// ----------------------------------------------------------------------------
// API model → feature model

export function toWorkshopSession(apiModel: WorkshopSessionApiModel): WorkshopSession {
  return {
    id: apiModel.id,
    customerId: apiModel.customerId,
    startTime: parseISO(apiModel.startTime),
    closeTime: apiModel.closeTime ? parseISO(apiModel.closeTime) : null,
    status: apiModel.status,
    workshopUsages: apiModel.workshopUsages.map(toWorkshopUsage),
    totalWorkshopDurationInMinutes: apiModel.totalWorkshopDurationInMinutes,
    totalWorkshopUnits: apiModel.totalWorkshopUnits,
    toolUsageSummaries: apiModel.toolUsageSummaries.map(toToolUsageSummary),
  };
}

function toWorkshopUsage(apiModel: WorkshopUsageApiModel): WorkshopUsage {
  return {
    id: apiModel.id,
    entryTime: parseISO(apiModel.entryTime),
    exitTime: apiModel.exitTime ? parseISO(apiModel.exitTime) : null,
    durationInMinutes: apiModel.durationInMinutes,
    unitsUsed: apiModel.unitsUsed,
  };
}

function toToolUsageSummary(apiModel: ToolUsageSummaryApiModel): ToolUsageSummary {
  return {
    toolId: apiModel.toolId,
    toolName: apiModel.toolName,
    active: apiModel.active,
    totalDurationInMinutes: apiModel.totalDurationInMinutes,
    totalUnits: apiModel.totalUnits,
    usages: apiModel.usages.map(toToolUsage),
  };
}

function toToolUsage(apiModel: ToolUsageDetailApiModel): ToolUsage {
  return {
    id: apiModel.id,
    startTime: parseISO(apiModel.startTime),
    endTime: apiModel.endTime ? parseISO(apiModel.endTime) : null,
    durationInMinutes: apiModel.durationInMinutes,
    unitsUsed: apiModel.unitsUsed,
  };
}
