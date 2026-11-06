import { describe, expect, it } from 'vitest';

import { WlanRelaisType } from '../shared/models/wlan-relais-type';
import { WlanRelaisTypeKeyPipe } from './wlan-relais-type-key.pipe';

describe('WlanRelaisTypeKeyPipe', () => {
  const pipe = new WlanRelaisTypeKeyPipe();

  it('should return the translation key of the type', () => {
    expect(pipe.transform(WlanRelaisType.ShellyPro)).toBe('tools.wlanRelaisTypes.SHELLY_PRO');
  });

  it('should return null for a tool without relais, so orDash shows a dash', () => {
    expect(pipe.transform(null)).toBeNull();
  });
});
