import { parseISO } from 'date-fns';

import {
  ChangeRfidReaderTypeRequest,
  RfidReaderApiModel,
  UpdateRfidReaderRequest,
} from '../shared/api/rfid-readers/rfid-reader.api-model';
import { ToolApiModel } from '../shared/api/tools/tool.api-model';
import {
  AssignedTool,
  ChangeRfidReaderTypeCommand,
  RfidReader,
  RfidReaderListItem,
  UpdateRfidReaderCommand,
} from './rfid-reader.model';

// ----------------------------------------------------------------------------
// API model → feature model

export function toRfidReader(apiModel: RfidReaderApiModel): RfidReader {
  return {
    id: apiModel.id,
    macAddress: apiModel.macAddress,
    type: apiModel.type,
    name: apiModel.name,
    socketName: apiModel.socketName,
    createdAt: parseISO(apiModel.createdAt),
  };
}

// a switch box gets the tool whose `rfidReaderId` points to it
export function toRfidReaderListItems(rfidReaders: RfidReaderApiModel[], tools: ToolApiModel[]): RfidReaderListItem[] {
  const toolsByRfidReaderId = new Map(
    tools.filter((tool) => tool.rfidReaderId).map((tool) => [tool.rfidReaderId, toAssignedTool(tool)]),
  );
  return rfidReaders.map((rfidReader) => ({
    ...toRfidReader(rfidReader),
    assignedTool: toolsByRfidReaderId.get(rfidReader.id) ?? null,
  }));
}

function toAssignedTool(tool: ToolApiModel): AssignedTool {
  return { id: tool.id, name: tool.name };
}

// ----------------------------------------------------------------------------
// command → request

export function toUpdateRfidReaderRequest(command: UpdateRfidReaderCommand): UpdateRfidReaderRequest {
  return {
    name: command.name ?? null,
    socketName: command.socketName ?? null,
  };
}

export function toChangeRfidReaderTypeRequest(command: ChangeRfidReaderTypeCommand): ChangeRfidReaderTypeRequest {
  return {
    type: command.type,
  };
}
