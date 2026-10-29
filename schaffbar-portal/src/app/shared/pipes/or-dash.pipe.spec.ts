import { describe, expect, it } from 'vitest';

import { OrDashPipe } from './or-dash.pipe';

describe('OrDashPipe', () => {
  const pipe = new OrDashPipe();

  it('should show a dash for a missing value', () => {
    expect(pipe.transform(null)).toBe('–');
    expect(pipe.transform(undefined)).toBe('–');
    expect(pipe.transform('')).toBe('–');
  });

  it('should pass a present value through, including 0', () => {
    expect(pipe.transform('Reader 1')).toBe('Reader 1');
    expect(pipe.transform(0)).toBe(0);
    expect(pipe.transform(42)).toBe(42);
  });
});
