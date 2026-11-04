import * as z from 'zod';

/**
 * Validates a backend response against its schema – as `parse` option of `httpResource` or in `map` of a plain
 * observable. Unknown fields are dropped; a mismatch is logged with the endpoint and thrown, so a resource ends up in
 * its error state.
 */
export function parseWith<T>(schema: z.ZodType<T>, endpoint: string): (response: unknown) => T {
  return (response) => {
    const result = schema.safeParse(response);
    if (!result.success) {
      console.error(`Unexpected response from ${endpoint}:\n${z.prettifyError(result.error)}`);
      throw result.error;
    }
    return result.data;
  };
}
