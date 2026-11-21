import * as z from 'zod';

export const activeWorkshopUserSchema = z.object({
  customerId: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  entryTime: z.iso.datetime(),
});

export type ActiveWorkshopUserApiModel = z.infer<typeof activeWorkshopUserSchema>;

export const activeToolUsageSchema = z.object({
  toolId: z.string(),
  toolName: z.string(),
  customerId: z.string(),
  firstName: z.string(),
  lastName: z.string(),
});

export type ActiveToolUsageApiModel = z.infer<typeof activeToolUsageSchema>;
