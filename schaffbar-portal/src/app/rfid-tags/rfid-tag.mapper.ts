import { RfidTagApiModel } from '../shared/api/rfid-tags/rfid-tag.api-model';
import { RfidTag } from './rfid-tag.model';

export function toRfidTag(apiModel: RfidTagApiModel): RfidTag {
  return {
    id: apiModel.id,
    active: apiModel.active,
  };
}
