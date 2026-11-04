import * as z from 'zod';

export const rfidTagSchema = z.object({
  id: z.string(),
  active: z.boolean(),
});

export type RfidTagApiModel = z.infer<typeof rfidTagSchema>;
