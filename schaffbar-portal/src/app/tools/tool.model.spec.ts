import { describe, expect, it } from 'vitest';

import { WlanRelaisType } from '../shared/models/wlan-relais-type';
import { CertificationRequirement } from '../shared/models/certification-requirement';
import { ToolArea } from '../shared/models/tool-area';
import { isHardwareSetUp, Tool, wlanRelaisCommands } from './tool.model';

describe('wlanRelaisCommands', () => {
  it('should fill in the IP address of the relais', () => {
    expect(wlanRelaisCommands(WlanRelaisType.Shelly1, '192.168.0.10')).toEqual({
      httpStartCommand: 'http://192.168.0.10/relay/0?',
      onCommand: 'turn=on',
      offCommand: 'turn=off',
    });
  });

  it('should use the RPC commands of a Shelly Pro', () => {
    expect(wlanRelaisCommands(WlanRelaisType.ShellyPro, '10.0.0.1')).toEqual({
      httpStartCommand: 'http://10.0.0.1/rpc/Switch.Set?',
      onCommand: 'id=0&on=true',
      offCommand: 'id=0&on=false',
    });
  });
});

describe('isHardwareSetUp', () => {
  const tool: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
    description: null,
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Red,
    rfidReaderId: null,
    wlanRelaisType: null,
    ipAddress: null,
    httpStartCommand: null,
    onCommand: null,
    offCommand: null,
    instructors: [],
  };

  it('should be true with switch box and WLAN relais', () => {
    expect(isHardwareSetUp({ ...tool, rfidReaderId: 'reader-1', wlanRelaisType: WlanRelaisType.Shelly1 })).toBe(true);
  });

  it('should be false when one of them is missing', () => {
    expect(isHardwareSetUp({ ...tool, rfidReaderId: 'reader-1' })).toBe(false);
    expect(isHardwareSetUp({ ...tool, wlanRelaisType: WlanRelaisType.Shelly1 })).toBe(false);
    expect(isHardwareSetUp(tool)).toBe(false);
  });
});
