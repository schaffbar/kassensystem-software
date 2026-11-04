import * as z from 'zod';

export const toolDashboardEntrySchema = z.object({
  toolId: z.string(),
  toolName: z.string(),
  customerId: z.string(),
  firstName: z.string(),
  lastName: z.string(),
});

export type ToolDashboardEntryApiModel = z.infer<typeof toolDashboardEntrySchema>;
