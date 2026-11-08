import { Pipe, PipeTransform } from '@angular/core';

import { RfidTagState } from './rfid-tag.model';

/**
 * Translation key of the state of a tag.
 *
 * `{{ rfidTag.state | rfidTagStateKey | translate }}`
 */
@Pipe({ name: 'rfidTagStateKey' })
export class RfidTagStateKeyPipe implements PipeTransform {
  transform(state: RfidTagState): string {
    return `rfidTags.states.${state}`;
  }
}
