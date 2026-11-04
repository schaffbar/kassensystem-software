import * as z from 'zod';

import { WorkshopSessionStatus } from '../../models/workshop-session-status';

export const workshopUsageSchema = z.object({
  id: z.string(),
  entryTime: z.iso.datetime(),
  exitTime: z.iso.datetime().nullable(),
  durationInMinutes: z.number().nullable(),
  unitsUsed: z.number().nullable(),
});

export const toolUsageDetailSchema = z.object({
  id: z.string(),
  startTime: z.iso.datetime(),
  endTime: z.iso.datetime().nullable(),
  durationInMinutes: z.number().nullable(),
  unitsUsed: z.number().nullable(),
});

export const toolUsageSummarySchema = z.object({
  toolId: z.string(),
  toolName: z.string().nullable(),
  active: z.boolean(),
  totalDurationInMinutes: z.number(),
  totalUnits: z.number(),
  usages: z.array(toolUsageDetailSchema),
});

export const workshopSessionSchema = z.object({
  id: z.string(),
  customerId: z.string(),
  startTime: z.iso.datetime(),
  closeTime: z.iso.datetime().nullable(),
  status: z.enum(WorkshopSessionStatus),
  totalWorkshopDurationInMinutes: z.number(),
  totalWorkshopUnits: z.number(),
  workshopUsages: z.array(workshopUsageSchema),
  toolUsageSummaries: z.array(toolUsageSummarySchema),
});

export type WorkshopUsageApiModel = z.infer<typeof workshopUsageSchema>;
export type ToolUsageDetailApiModel = z.infer<typeof toolUsageDetailSchema>;
export type ToolUsageSummaryApiModel = z.infer<typeof toolUsageSummarySchema>;
export type WorkshopSessionApiModel = z.infer<typeof workshopSessionSchema>;
