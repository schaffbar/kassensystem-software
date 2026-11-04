import { describe, expect, it } from 'vitest';

import { toRfidTag } from './rfid-tag.mapper';

describe('toRfidTag', () => {
  it('should map the API model', () => {
    expect(toRfidTag({ id: 'tag-1', active: true })).toEqual({ id: 'tag-1', active: true });
  });
});
