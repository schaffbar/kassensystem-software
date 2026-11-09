import { describe, expect, it } from 'vitest';

import { RfidReaderApiModel } from '../shared/api/rfid-readers/rfid-reader.api-model';
import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { RfidReaderType } from '../shared/models/rfid-reader-type';
import {
  toChangeRfidReaderTypeRequest,
  toRfidReader,
  toRfidReaderListItem,
  toRfidReaderListItems,
  toUpdateRfidReaderRequest,
} from './rfid-reader.mapper';

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

  describe('toRfidReaderListItems', () => {
    const box: RfidReaderApiModel = {
      id: 'box-1',
      macAddress: 'AA:BB',
      type: RfidReaderType.SwitchBox,
      name: 'Schaltbox Bandsäge',
      socketName: 'box-bandsaege',
      createdAt: '2026-01-01T10:00:00Z',
    };
    const freeBox: RfidReaderApiModel = { ...box, id: 'box-2', name: 'Schaltbox 07' };

    it('should add the tool that uses the reader as switch box', () => {
      const tools = [
        toolFixture({ id: 'tool-1', name: 'Bandsäge', rfidReaderId: 'box-1' }),
        toolFixture({ id: 'tool-2', name: 'Lötstation', rfidReaderId: null }),
      ];

      const items = toRfidReaderListItems([box, freeBox], tools);

      expect(items).toEqual([
        { ...toRfidReader(box), assignedTool: { id: 'tool-1', name: 'Bandsäge' } },
        { ...toRfidReader(freeBox), assignedTool: null },
      ]);
    });

    it('should add the tool to a single reader', () => {
      const tools = [toolFixture({ id: 'tool-1', name: 'Bandsäge', rfidReaderId: 'box-1' })];

      expect(toRfidReaderListItem(box, tools).assignedTool).toEqual({ id: 'tool-1', name: 'Bandsäge' });
      expect(toRfidReaderListItem(freeBox, tools).assignedTool).toBeNull();
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
