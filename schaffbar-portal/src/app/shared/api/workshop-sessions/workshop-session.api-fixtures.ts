import { WorkshopSessionStatus } from '../../models/workshop-session-status';
import { ToolUsageSummaryApiModel, WorkshopSessionApiModel, WorkshopUsageApiModel } from './workshop-session.api-model';

export function workshopUsageFixture(overrides: Partial<WorkshopUsageApiModel> = {}): WorkshopUsageApiModel {
  return {
    id: 'usage-1',
    entryTime: '2026-10-31T10:00:00Z',
    exitTime: '2026-10-31T11:00:00Z',
    durationInMinutes: 60,
    unitsUsed: 10,
    ...overrides,
  };
}

export function toolUsageSummaryFixture(overrides: Partial<ToolUsageSummaryApiModel> = {}): ToolUsageSummaryApiModel {
  return {
    toolId: 'tool-1',
    toolName: 'Bandsäge',
    active: false,
    totalDurationInMinutes: 31,
    totalUnits: 6,
    usages: [
      {
        id: 'tool-usage-1',
        startTime: '2026-10-31T10:05:00Z',
        endTime: '2026-10-31T10:36:00Z',
        durationInMinutes: 31,
        unitsUsed: 6,
      },
    ],
    ...overrides,
  };
}

export function workshopSessionFixture(overrides: Partial<WorkshopSessionApiModel> = {}): WorkshopSessionApiModel {
  return {
    id: 'session-1',
    customerId: 'user-1',
    startTime: '2026-10-31T10:00:00Z',
    closeTime: null,
    status: WorkshopSessionStatus.Open,
    totalWorkshopDurationInMinutes: 60,
    totalWorkshopUnits: 10,
    workshopUsages: [workshopUsageFixture()],
    toolUsageSummaries: [toolUsageSummaryFixture()],
    ...overrides,
  };
}
