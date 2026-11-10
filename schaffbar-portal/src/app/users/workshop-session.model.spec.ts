import { describe, expect, it } from 'vitest';

import { WorkshopSessionStatus } from '../shared/models/workshop-session-status';
import { canCloseWorkshopSession, isInWorkshop, WorkshopSession } from './workshop-session.model';

describe('workshop-session.model', () => {
  const at = (hours: number) => new Date(2026, 10, 9, hours);
  const checkedOut: WorkshopSession = {
    id: 'session-1',
    customerId: 'user-1',
    startTime: at(14),
    closeTime: null,
    status: WorkshopSessionStatus.Open,
    workshopUsages: [{ id: 'w-1', entryTime: at(14), exitTime: at(16), durationInMinutes: 120, unitsUsed: 2 }],
    totalWorkshopDurationInMinutes: 120,
    totalWorkshopUnits: 2,
    toolUsageSummaries: [],
  };

  it('should close a session after checking out', () => {
    expect(isInWorkshop(checkedOut)).toBe(false);
    expect(canCloseWorkshopSession(checkedOut)).toBe(true);
  });

  it('should not close a session while the user is in the workshop', () => {
    const inside: WorkshopSession = {
      ...checkedOut,
      workshopUsages: [
        ...checkedOut.workshopUsages,
        { id: 'w-2', entryTime: at(17), exitTime: null, durationInMinutes: null, unitsUsed: null },
      ],
    };

    expect(isInWorkshop(inside)).toBe(true);
    expect(canCloseWorkshopSession(inside)).toBe(false);
  });

  it('should not close a session while a tool is running', () => {
    const toolRunning: WorkshopSession = {
      ...checkedOut,
      toolUsageSummaries: [
        { toolId: 'tool-1', toolName: 'Bandsäge', active: true, totalDurationInMinutes: 0, totalUnits: 0, usages: [] },
      ],
    };

    expect(canCloseWorkshopSession(toolRunning)).toBe(false);
  });
});
