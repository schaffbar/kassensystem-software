import { Pipe, PipeTransform } from '@angular/core';

import { RfidReaderType } from '../shared/models/rfid-reader-type';

/**
 * Translation key of a reader type – `rfidReaders.types.none` for a new reader without type.
 *
 * `{{ reader.type | rfidReaderTypeKey | translate }}`
 */
@Pipe({ name: 'rfidReaderTypeKey' })
export class RfidReaderTypeKeyPipe implements PipeTransform {
  transform(type: RfidReaderType | null): string {
    return type ? `rfidReaders.types.${type.toLowerCase()}` : 'rfidReaders.types.none';
  }
}
