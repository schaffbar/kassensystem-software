import { describe, expect, it } from 'vitest';

import {
  toolUsageSummaryFixture,
  workshopSessionFixture,
  workshopUsageFixture,
} from '../shared/api/workshop-sessions/workshop-session.api-fixtures';
import { WorkshopSessionStatus } from '../shared/models/workshop-session-status';
import { toWorkshopSession } from './workshop-session.mapper';

describe('toWorkshopSession', () => {
  it('should map the session with its usages and parse all times', () => {
    expect(toWorkshopSession(workshopSessionFixture())).toEqual({
      id: 'session-1',
      customerId: 'user-1',
      startTime: new Date('2026-10-31T10:00:00Z'),
      closeTime: null,
      status: WorkshopSessionStatus.Open,
      totalWorkshopDurationInMinutes: 60,
      totalWorkshopUnits: 10,
      workshopUsages: [
        {
          id: 'usage-1',
          entryTime: new Date('2026-10-31T10:00:00Z'),
          exitTime: new Date('2026-10-31T11:00:00Z'),
          durationInMinutes: 60,
          unitsUsed: 10,
        },
      ],
      toolUsageSummaries: [
        {
          toolId: 'tool-1',
          toolName: 'Bandsäge',
          active: false,
          totalDurationInMinutes: 31,
          totalUnits: 6,
          usages: [
            {
              id: 'tool-usage-1',
              startTime: new Date('2026-10-31T10:05:00Z'),
              endTime: new Date('2026-10-31T10:36:00Z'),
              durationInMinutes: 31,
              unitsUsed: 6,
            },
          ],
        },
      ],
    });
  });

  it('should keep missing end times and values of running usages as null', () => {
    const session = toWorkshopSession(
      workshopSessionFixture({
        workshopUsages: [workshopUsageFixture({ exitTime: null, durationInMinutes: null, unitsUsed: null })],
        toolUsageSummaries: [
          toolUsageSummaryFixture({
            toolName: null,
            active: true,
            usages: [
              { id: 'u', startTime: '2026-10-31T10:05:00Z', endTime: null, durationInMinutes: null, unitsUsed: null },
            ],
          }),
        ],
      }),
    );

    expect(session.workshopUsages[0]).toMatchObject({ exitTime: null, durationInMinutes: null, unitsUsed: null });
    expect(session.toolUsageSummaries[0]).toMatchObject({ toolName: null, active: true });
    expect(session.toolUsageSummaries[0].usages[0].endTime).toBeNull();
  });
});
