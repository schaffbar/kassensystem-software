import { describe, expect, it } from 'vitest';

import { WlanRelaisType } from '../shared/models/wlan-relais-type';
import { wlanRelaisCommands } from './tool.model';

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
