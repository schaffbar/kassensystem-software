import { Pipe, PipeTransform } from '@angular/core';

import { RfidReaderType } from '../shared/models/rfid-reader-type';

const ICONS: Record<RfidReaderType, string> = {
  [RfidReaderType.GateKeeperIn]: 'meeting_room',
  [RfidReaderType.GateKeeperOut]: 'meeting_room',
  [RfidReaderType.RfidTagRegister]: 'sell',
  [RfidReaderType.RfidTagAssigner]: 'sell',
  [RfidReaderType.SwitchBox]: 'electrical_services',
};

/**
 * Material icon of a reader type, like in the prototype: a door for the gates, a tag at the counter, a plug for a
 * switch box – the general reader icon for a reader without type.
 *
 * `<mat-icon>{{ reader.type | rfidReaderTypeIcon }}</mat-icon>`
 */
@Pipe({ name: 'rfidReaderTypeIcon' })
export class RfidReaderTypeIconPipe implements PipeTransform {
  transform(type: RfidReaderType | null): string {
    return type ? ICONS[type] : 'sensors';
  }
}
