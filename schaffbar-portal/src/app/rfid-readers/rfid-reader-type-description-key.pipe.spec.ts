import { describe, expect, it } from 'vitest';

import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { RfidReaderTypeDescriptionKeyPipe } from './rfid-reader-type-description-key.pipe';

describe('RfidReaderTypeDescriptionKeyPipe', () => {
  const pipe = new RfidReaderTypeDescriptionKeyPipe();

  it('should return the translation key of the description of the type', () => {
    expect(pipe.transform(RfidReaderType.SwitchBox)).toBe('rfidReaders.typeDescriptions.switch_box');
    expect(pipe.transform(RfidReaderType.RfidTagAssigner)).toBe('rfidReaders.typeDescriptions.rfid_tag_assigner');
  });

  it('should return the "none" key for a reader without type', () => {
    expect(pipe.transform(null)).toBe('rfidReaders.typeDescriptions.none');
  });
});
