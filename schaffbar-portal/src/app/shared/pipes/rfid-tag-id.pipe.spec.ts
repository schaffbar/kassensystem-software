import { describe, expect, it } from 'vitest';

import { RfidTagIdPipe } from './rfid-tag-id.pipe';

describe('RfidTagIdPipe', () => {
  const pipe = new RfidTagIdPipe();

  it('should group the ID in blocks of four', () => {
    expect(pipe.transform('04A31F9C')).toBe('04A3 1F9C');
    expect(pipe.transform('04A31F9C2B8E1D')).toBe('04A3 1F9C 2B8E 1D');
  });

  it('should keep a short or empty ID', () => {
    expect(pipe.transform('04A3')).toBe('04A3');
    expect(pipe.transform('')).toBe('');
  });
});
