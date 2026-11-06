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
