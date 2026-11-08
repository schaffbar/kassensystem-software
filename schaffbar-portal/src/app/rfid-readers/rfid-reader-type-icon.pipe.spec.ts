import { describe, expect, it } from 'vitest';

import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { RfidReaderTypeIconPipe } from './rfid-reader-type-icon.pipe';

describe('RfidReaderTypeIconPipe', () => {
  const pipe = new RfidReaderTypeIconPipe();

  it('should return a door for the gates and a tag at the counter', () => {
    expect(pipe.transform(RfidReaderType.GateKeeperIn)).toBe('meeting_room');
    expect(pipe.transform(RfidReaderType.GateKeeperOut)).toBe('meeting_room');
    expect(pipe.transform(RfidReaderType.RfidTagRegister)).toBe('sell');
    expect(pipe.transform(RfidReaderType.RfidTagAssigner)).toBe('sell');
  });

  it('should return a plug for a switch box', () => {
    expect(pipe.transform(RfidReaderType.SwitchBox)).toBe('electrical_services');
  });

  it('should return the general reader icon for a reader without type', () => {
    expect(pipe.transform(null)).toBe('sensors');
  });
});
