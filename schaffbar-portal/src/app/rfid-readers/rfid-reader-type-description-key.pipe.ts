import { Pipe, PipeTransform } from '@angular/core';

import { RfidReaderType } from '../shared/models/rfid-reader-type';

/**
 * Translation key of the description of a reader type – what the reader does when a tag is placed on it.
 *
 * `{{ reader.type | rfidReaderTypeDescriptionKey | translate }}`
 */
@Pipe({ name: 'rfidReaderTypeDescriptionKey' })
export class RfidReaderTypeDescriptionKeyPipe implements PipeTransform {
  transform(type: RfidReaderType | null): string {
    return type ? `rfidReaders.typeDescriptions.${type.toLowerCase()}` : 'rfidReaders.typeDescriptions.none';
  }
}
