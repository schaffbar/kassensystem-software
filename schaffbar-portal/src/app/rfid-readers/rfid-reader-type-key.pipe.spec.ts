import { describe, expect, it } from 'vitest';

import { RfidReaderType } from './rfid-reader.model';
import { RfidReaderTypeKeyPipe } from './rfid-reader-type-key.pipe';

describe('RfidReaderTypeKeyPipe', () => {
  const pipe = new RfidReaderTypeKeyPipe();

  it('should return the translation key of the type', () => {
    expect(pipe.transform(RfidReaderType.SwitchBox)).toBe('rfidReaders.types.switch_box');
    expect(pipe.transform(RfidReaderType.GateKeeperIn)).toBe('rfidReaders.types.gate_keeper_in');
  });

  it('should return the "none" key for a reader without type', () => {
    expect(pipe.transform(null)).toBe('rfidReaders.types.none');
  });
});
