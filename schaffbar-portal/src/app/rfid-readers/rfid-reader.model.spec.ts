import { describe, expect, it } from 'vitest';

import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { canDeleteRfidReader, isUnassigned, rfidReaderGroup, RfidReaderListItem } from './rfid-reader.model';

describe('rfidReaderGroup', () => {
  it('should put readers without type into the new ones', () => {
    expect(rfidReaderGroup(null)).toBe('new');
  });

  it('should put the gates and the counter readers together', () => {
    expect(rfidReaderGroup(RfidReaderType.GateKeeperIn)).toBe('desk');
    expect(rfidReaderGroup(RfidReaderType.GateKeeperOut)).toBe('desk');
    expect(rfidReaderGroup(RfidReaderType.RfidTagRegister)).toBe('desk');
    expect(rfidReaderGroup(RfidReaderType.RfidTagAssigner)).toBe('desk');
  });

  it('should keep the switch boxes apart', () => {
    expect(rfidReaderGroup(RfidReaderType.SwitchBox)).toBe('switchBox');
  });
});

describe('isUnassigned', () => {
  const switchBox: RfidReaderListItem = {
    id: 'box-1',
    macAddress: 'AA:BB',
    type: RfidReaderType.SwitchBox,
    name: 'Schaltbox 07',
    socketName: null,
    createdAt: new Date('2026-01-01T10:00:00Z'),
    assignedTool: null,
  };

  it('should be true for a switch box without tool and for a reader without type', () => {
    expect(isUnassigned(switchBox)).toBe(true);
    expect(isUnassigned({ ...switchBox, type: null })).toBe(true);
  });

  it('should be false for a switch box with tool and for the gates and the counter', () => {
    expect(isUnassigned({ ...switchBox, assignedTool: { id: 'tool-1', name: 'Bandsäge' } })).toBe(false);
    expect(isUnassigned({ ...switchBox, type: RfidReaderType.GateKeeperIn })).toBe(false);
  });
});

describe('canDeleteRfidReader', () => {
  const switchBox: RfidReaderListItem = {
    id: 'box-1',
    macAddress: 'AA:BB',
    type: RfidReaderType.SwitchBox,
    name: 'Schaltbox 07',
    socketName: null,
    createdAt: new Date('2026-01-01T10:00:00Z'),
    assignedTool: null,
  };

  it('should allow it only without an assigned tool', () => {
    expect(canDeleteRfidReader(switchBox)).toBe(true);
    expect(canDeleteRfidReader({ ...switchBox, assignedTool: { id: 'tool-1', name: 'Bandsäge' } })).toBe(false);
  });
});
