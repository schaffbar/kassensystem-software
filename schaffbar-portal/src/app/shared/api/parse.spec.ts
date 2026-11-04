import { afterEach, describe, expect, it, vi } from 'vitest';
import * as z from 'zod';

import { parseWith } from './parse';

describe('parseWith', () => {
  const schema = z.object({ id: z.string(), name: z.string().nullable() });
  const parse = parseWith(schema, 'GET /things');

  afterEach(() => vi.restoreAllMocks());

  it('should return a valid response without unknown fields', () => {
    expect(parse({ id: '1', name: null, unknown: true })).toEqual({ id: '1', name: null });
  });

  it('should log the endpoint and the path and throw for an invalid response', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    expect(() => parse({ id: 1 })).toThrow(z.ZodError);
    expect(error).toHaveBeenCalledOnce();
    expect(error.mock.calls[0][0]).toContain('GET /things');
    expect(error.mock.calls[0][0]).toContain('at id');
    expect(error.mock.calls[0][0]).toContain('at name');
  });
});
