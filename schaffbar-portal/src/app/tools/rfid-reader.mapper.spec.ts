import { describe, expect, it } from 'vitest';

import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { toRfidReader } from './rfid-reader.mapper';

describe('toRfidReader', () => {
  it('should take only id, MAC address, name and type from the API model', () => {
    expect(
      toRfidReader({
        id: 'reader-1',
        macAddress: 'AA:BB:CC:DD:EE:FF',
        type: RfidReaderType.SwitchBox,
        name: 'Bandsäge',
        socketName: 'Steckdose 1',
        createdAt: '2026-01-01T10:00:00Z',
      }),
    ).toEqual({ id: 'reader-1', macAddress: 'AA:BB:CC:DD:EE:FF', name: 'Bandsäge', type: RfidReaderType.SwitchBox });
  });

  it('should keep a missing name and type as null', () => {
    expect(
      toRfidReader({
        id: 'reader-1',
        macAddress: 'AA:BB:CC:DD:EE:FF',
        type: null,
        name: null,
        socketName: null,
        createdAt: '2026-01-01T10:00:00Z',
      }),
    ).toEqual({ id: 'reader-1', macAddress: 'AA:BB:CC:DD:EE:FF', name: null, type: null });
  });
});
