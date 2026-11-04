import * as z from 'zod';

export const workshopDashboardEntrySchema = z.object({
  customerId: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  entryTime: z.iso.datetime(),
});

export type WorkshopDashboardEntryApiModel = z.infer<typeof workshopDashboardEntrySchema>;
