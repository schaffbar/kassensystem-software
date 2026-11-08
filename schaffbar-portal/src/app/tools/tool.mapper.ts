import { ToolDashboardEntryApiModel } from '../shared/api/tool-dashboard/tool-dashboard.api-model';
import {
  CreateToolRequest,
  InstructorsRequest,
  SetWlanRelaisRequest,
  ToolApiModel,
  UpdateToolRequest,
} from '../shared/api/tools/tool.api-model';
import {
  AddToolInstructorsCommand,
  CreateToolCommand,
  RemoveToolInstructorsCommand,
  SetWlanRelaisCommand,
  Tool,
  ToolListItem,
  UpdateToolCommand,
} from './tool.model';

// ----------------------------------------------------------------------------
// API model → feature model

export function toTool(apiModel: ToolApiModel): Tool {
  return {
    id: apiModel.id,
    name: apiModel.name,
    description: apiModel.description,
    area: apiModel.area,
    certificationRequirement: apiModel.certificationRequirement,
    rfidReaderId: apiModel.rfidReaderId,
    ipAddress: apiModel.ipAddress,
    wlanRelaisType: apiModel.wlanRelaisType,
    httpStartCommand: apiModel.httpStartCommand,
    onCommand: apiModel.onCommand,
    offCommand: apiModel.offCommand,
    instructors: apiModel.instructors,
  };
}

// a tool is in use while the dashboard lists it as active
export function toToolListItems(tools: ToolApiModel[], activeTools: ToolDashboardEntryApiModel[]): ToolListItem[] {
  const usersByToolId = new Map(
    activeTools.map((activeTool) => [activeTool.toolId, `${activeTool.firstName} ${activeTool.lastName}`]),
  );
  return tools.map((tool) => ({ ...toTool(tool), usedBy: usersByToolId.get(tool.id) ?? null }));
}

// ----------------------------------------------------------------------------
// command → request

export function toCreateToolRequest(command: CreateToolCommand): CreateToolRequest {
  return {
    name: command.name,
    description: command.description ?? null,
    area: command.area,
    certificationRequirement: command.certificationRequirement,
  };
}

export function toUpdateToolRequest(command: UpdateToolCommand): UpdateToolRequest {
  return {
    name: command.name,
    description: command.description ?? null,
    area: command.area,
    certificationRequirement: command.certificationRequirement,
  };
}

export function toSetWlanRelaisRequest(command: SetWlanRelaisCommand): SetWlanRelaisRequest {
  return {
    wlanRelaisType: command.wlanRelaisType,
    ipAddress: command.ipAddress,
  };
}

export function toInstructorsRequest(
  command: AddToolInstructorsCommand | RemoveToolInstructorsCommand,
): InstructorsRequest {
  return {
    instructorIds: command.instructorIds,
  };
}
