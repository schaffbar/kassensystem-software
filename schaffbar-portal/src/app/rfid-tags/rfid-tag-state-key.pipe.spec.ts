import { describe, expect, it } from 'vitest';

import { RfidTagStateKeyPipe } from './rfid-tag-state-key.pipe';

describe('RfidTagStateKeyPipe', () => {
  const pipe = new RfidTagStateKeyPipe();

  it('should return the translation key of the state', () => {
    expect(pipe.transform(true)).toBe('rfidTags.states.active');
    expect(pipe.transform(false)).toBe('rfidTags.states.inactive');
  });
});
