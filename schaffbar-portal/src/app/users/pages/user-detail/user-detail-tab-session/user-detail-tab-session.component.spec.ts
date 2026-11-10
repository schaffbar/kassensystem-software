import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { WorkshopSessionStatus } from '../../../../shared/models/workshop-session-status';
import { UserOpenSessionComponent } from '../../../components/user-open-session/user-open-session.component';
import { ToolUsage, WorkshopSession } from '../../../workshop-session.model';
import { UserDetailStore } from '../user-detail.store';
import { UserDetailTabSessionComponent } from './user-detail-tab-session.component';

describe('UserDetailTabSessionComponent', () => {
  const at = (hours: number, minutes: number) => new Date(2026, 10, 9, hours, minutes);
  const toolUsage = (id: string, start: Date, end: Date | null): ToolUsage => ({
    id,
    startTime: start,
    endTime: end,
    durationInMinutes: end ? 5 : null,
    unitsUsed: end ? 1 : null,
  });
  // two stays: 17:49–19:04 and since 19:49; the tools in mixed order, as the backend sends them per tool
  const session: WorkshopSession = {
    id: 'session-1',
    customerId: 'user-1',
    startTime: at(17, 49),
    closeTime: null,
    status: WorkshopSessionStatus.Open,
    workshopUsages: [
      { id: 'w-2', entryTime: at(19, 49), exitTime: null, durationInMinutes: null, unitsUsed: null },
      { id: 'w-1', entryTime: at(17, 49), exitTime: at(19, 4), durationInMinutes: 75, unitsUsed: 2 },
    ],
    totalWorkshopDurationInMinutes: 100,
    totalWorkshopUnits: 3,
    toolUsageSummaries: [
      {
        toolId: 'tool-1',
        toolName: 'Kreissäge',
        active: true,
        totalDurationInMinutes: 29,
        totalUnits: 3,
        usages: [
          toolUsage('k-1', at(17, 50), at(17, 54)),
          toolUsage('k-2', at(18, 30), at(18, 55)),
          toolUsage('k-3', at(19, 50), null),
        ],
      },
      {
        toolId: 'tool-2',
        toolName: 'Bandsäge',
        active: false,
        totalDurationInMinutes: 14,
        totalUnits: 1,
        usages: [toolUsage('b-1', at(18, 20), at(18, 34))],
      },
    ],
  };

  let fixture: ComponentFixture<UserDetailTabSessionComponent>;
  let openSession: WritableSignal<WorkshopSession | null>;

  beforeEach(async () => {
    openSession = signal(session);

    await TestBed.configureTestingModule({
      imports: [UserDetailTabSessionComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: UserDetailStore, useValue: { openSession } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailTabSessionComponent);
    await fixture.whenStable();
  });

  function viewModel() {
    return fixture.debugElement.query(By.directive(UserOpenSessionComponent)).componentInstance.userOpenSession();
  }

  it('should list the stays oldest first, each with the tools started during it in their order', () => {
    const stays = viewModel().stays;

    expect(stays.map((stay: { id: string }) => stay.id)).toEqual(['w-1', 'w-2']);
    expect(stays[0].toolUsages.map((usage: { id: string }) => usage.id)).toEqual(['k-1', 'b-1', 'k-2']);
    expect(stays[1].toolUsages).toEqual([
      { id: 'k-3', toolName: 'Kreissäge', startTime: at(19, 50), endTime: null, durationInMinutes: null, units: null },
    ]);
    expect(stays[0]).toEqual(
      expect.objectContaining({ startTime: at(17, 49), endTime: at(19, 4), durationInMinutes: 75, units: 2 }),
    );
  });

  it('should add a tool usage outside of every stay to the last stay before it', async () => {
    openSession.set({
      ...session,
      toolUsageSummaries: [{ ...session.toolUsageSummaries[1], usages: [toolUsage('b-2', at(19, 20), at(19, 25))] }],
    });
    await fixture.whenStable();

    expect(viewModel().stays[0].toolUsages.map((usage: { id: string }) => usage.id)).toEqual(['b-2']);
  });

  it('should sum up the session and each tool, sorted by name', () => {
    expect(viewModel()).toEqual(
      expect.objectContaining({
        workshopMinutes: 100,
        workshopUnits: 3,
        toolUnits: 4,
        tools: [
          { toolId: 'tool-2', toolName: 'Bandsäge', usages: 1, minutes: 14, units: 1 },
          { toolId: 'tool-1', toolName: 'Kreissäge', usages: 3, minutes: 29, units: 3 },
        ],
      }),
    );
  });

  it('should show an empty state without an open session', async () => {
    openSession.set(null);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('schbar-user-open-session')).toBeNull();
    expect(fixture.nativeElement.querySelector('schbar-empty-state')).not.toBeNull();
  });
});
