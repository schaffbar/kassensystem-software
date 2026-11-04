import { parseISO } from 'date-fns';

import {
  ChangeRfidReaderTypeRequest,
  RfidReaderApiModel,
  UpdateRfidReaderRequest,
} from '../shared/api/rfid-readers/rfid-reader.api-model';
import { ChangeRfidReaderTypeCommand, RfidReader, UpdateRfidReaderCommand } from './rfid-reader.model';

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
