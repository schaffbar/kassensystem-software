import { describe, expect, it } from 'vitest';

import { AgeGroup } from '../shared/models/age-group';
import { RfidReaderType } from '../shared/models/rfid-reader-type';
import { ToolArea } from '../shared/models/tool-area';
import { WlanRelaisType } from '../shared/models/wlan-relais-type';
import { isHardwareSetUp, isMinor, needsRfidReaderType, Tool } from './dashboard.model';

describe('dashboard.model', () => {
  const tool: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
    area: ToolArea.Holz,
    rfidReaderId: 'reader-1',
    wlanRelaisType: WlanRelaisType.Shelly1,
  };

  it('should need switch box and WLAN relais for the hardware', () => {
    expect(isHardwareSetUp(tool)).toBe(true);
    expect(isHardwareSetUp({ ...tool, rfidReaderId: null })).toBe(false);
    expect(isHardwareSetUp({ ...tool, wlanRelaisType: null })).toBe(false);
  });

  it('should need a type only for a reader without one', () => {
    const rfidReader = { id: 'reader-1', name: null, macAddress: 'AA:BB', type: null };

    expect(needsRfidReaderType(rfidReader)).toBe(true);
    expect(needsRfidReaderType({ ...rfidReader, type: RfidReaderType.GateKeeperIn })).toBe(false);
  });

  it('should count everybody under 18 as minor', () => {
    expect(isMinor({ id: 'user-1', ageGroup: AgeGroup.Under16, clubMember: false })).toBe(true);
    expect(isMinor({ id: 'user-1', ageGroup: AgeGroup.Under18, clubMember: false })).toBe(true);
    expect(isMinor({ id: 'user-1', ageGroup: AgeGroup.Adult, clubMember: false })).toBe(false);
  });
});
