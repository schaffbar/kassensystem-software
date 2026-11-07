import { Pipe, PipeTransform } from '@angular/core';

/**
 * Translation key of the state of an RFID tag.
 *
 * `{{ rfidTag.active | rfidTagStateKey | translate }}`
 */
@Pipe({ name: 'rfidTagStateKey' })
export class RfidTagStateKeyPipe implements PipeTransform {
  transform(active: boolean): string {
    return active ? 'rfidTags.states.active' : 'rfidTags.states.inactive';
  }
}
