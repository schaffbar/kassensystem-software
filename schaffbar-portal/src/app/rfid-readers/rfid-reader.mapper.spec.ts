import { describe, expect, it } from 'vitest';

import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { toChangeRfidReaderTypeRequest, toRfidReader, toUpdateRfidReaderRequest } from './rfid-reader.mapper';

describe('rfid-reader.mapper', () => {
  describe('toRfidReader', () => {
    it('should map the API model and parse the creation date', () => {
      const rfidReader = toRfidReader({
        id: 'reader-1',
        macAddress: 'AA:BB',
        type: null,
        name: null,
        socketName: 'Steckdose 1',
        createdAt: '2026-01-01T10:00:00Z',
      });

      expect(rfidReader).toEqual({
        id: 'reader-1',
        macAddress: 'AA:BB',
        type: null,
        name: null,
        socketName: 'Steckdose 1',
        createdAt: new Date('2026-01-01T10:00:00Z'),
      });
    });
  });

  describe('toUpdateRfidReaderRequest', () => {
    it('should send missing values as null and no id', () => {
      expect(toUpdateRfidReaderRequest({ rfidReaderId: 'reader-1', name: 'Reader 1' })).toEqual({
        name: 'Reader 1',
        socketName: null,
      });
    });
  });

  describe('toChangeRfidReaderTypeRequest', () => {
    it('should send only the type', () => {
      expect(toChangeRfidReaderTypeRequest({ rfidReaderId: 'reader-1', type: RfidReaderType.SwitchBox })).toEqual({
        type: RfidReaderType.SwitchBox,
      });
    });
  });
});
