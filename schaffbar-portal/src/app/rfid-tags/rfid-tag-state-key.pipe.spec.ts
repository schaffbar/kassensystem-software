import { describe, expect, it } from 'vitest';

import { RfidTagStateKeyPipe } from './rfid-tag-state-key.pipe';

describe('RfidTagStateKeyPipe', () => {
  const pipe = new RfidTagStateKeyPipe();

  it('should return the translation key of the state', () => {
    expect(pipe.transform('assigned')).toBe('rfidTags.states.assigned');
    expect(pipe.transform('free')).toBe('rfidTags.states.free');
    expect(pipe.transform('deactivated')).toBe('rfidTags.states.deactivated');
  });
});
