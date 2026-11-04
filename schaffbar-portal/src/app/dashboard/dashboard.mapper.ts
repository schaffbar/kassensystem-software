import { parseISO } from 'date-fns';

import { ToolDashboardEntryApiModel } from '../shared/api/tool-dashboard/tool-dashboard.api-model';
import { WorkshopDashboardEntryApiModel } from '../shared/api/workshop-dashboard/workshop-dashboard.api-model';
import { ActiveTool, ActiveUser } from './dashboard.model';

export function toActiveUser(apiModel: WorkshopDashboardEntryApiModel): ActiveUser {
  return {
    customerId: apiModel.customerId,
    firstName: apiModel.firstName,
    lastName: apiModel.lastName,
    entryTime: parseISO(apiModel.entryTime),
  };
}

export function toActiveTool(apiModel: ToolDashboardEntryApiModel): ActiveTool {
  return {
    toolId: apiModel.toolId,
    toolName: apiModel.toolName,
    customerId: apiModel.customerId,
    firstName: apiModel.firstName,
    lastName: apiModel.lastName,
  };
}
