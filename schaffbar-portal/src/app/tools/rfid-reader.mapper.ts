import { RfidReaderApiModel } from '../shared/api/rfid-readers/rfid-reader.api-model';
import { RfidReader } from './rfid-reader.model';

// ----------------------------------------------------------------------------
// API model → feature model

export function toRfidReader(apiModel: RfidReaderApiModel): RfidReader {
  return {
    id: apiModel.id,
    macAddress: apiModel.macAddress,
    name: apiModel.name,
    type: apiModel.type,
  };
}
