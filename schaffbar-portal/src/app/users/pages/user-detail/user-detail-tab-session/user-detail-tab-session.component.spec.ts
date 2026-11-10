import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { WorkshopSessionStatus } from '../../../../shared/models/workshop-session-status';
import { UserOpenSessionComponent } from '../../../components/user-open-session/user-open-session.component';
import { WorkshopSession } from '../../../workshop-session.model';
import { UserDetailStore } from '../user-detail.store';
import { UserDetailTabSessionComponent } from './user-detail-tab-session.component';

describe('UserDetailTabSessionComponent', () => {
  const at = (hours: number, minutes: number) => new Date(2026, 10, 9, hours, minutes);
  // in the workshop since 14:05, used the band saw 14:12–14:31, the lathe since 14:40
  const session: WorkshopSession = {
    id: 'session-1',
    customerId: 'user-1',
    startTime: at(14, 5),
    closeTime: null,
    status: WorkshopSessionStatus.Open,
    workshopUsages: [{ id: 'w-1', entryTime: at(14, 5), exitTime: null, durationInMinutes: null, unitsUsed: null }],
    totalWorkshopDurationInMinutes: 83,
    totalWorkshopUnits: 2,
    toolUsageSummaries: [
      {
        toolId: 'tool-2',
        toolName: 'Drechselbank',
        active: true,
        totalDurationInMinutes: 48,
        totalUnits: 2,
        usages: [{ id: 't-2', startTime: at(14, 40), endTime: null, durationInMinutes: null, unitsUsed: null }],
      },
      {
        toolId: 'tool-1',
        toolName: 'Bandsäge',
        active: false,
        totalDurationInMinutes: 19,
        totalUnits: 1,
        usages: [{ id: 't-1', startTime: at(14, 12), endTime: at(14, 31), durationInMinutes: 19, unitsUsed: 1 }],
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

  it('should list workshop and tool usages in the order they started, with the totals', () => {
    const openSessionComponent: UserOpenSessionComponent = fixture.debugElement.query(
      By.directive(UserOpenSessionComponent),
    ).componentInstance;

    expect(openSessionComponent.userOpenSession()).toEqual({
      usages: [
        {
          id: 'w-1',
          kind: 'workshop',
          toolName: null,
          startTime: at(14, 5),
          endTime: null,
          durationInMinutes: null,
          units: null,
        },
        {
          id: 't-1',
          kind: 'tool',
          toolName: 'Bandsäge',
          startTime: at(14, 12),
          endTime: at(14, 31),
          durationInMinutes: 19,
          units: 1,
        },
        {
          id: 't-2',
          kind: 'tool',
          toolName: 'Drechselbank',
          startTime: at(14, 40),
          endTime: null,
          durationInMinutes: null,
          units: null,
        },
      ],
      workshopMinutes: 83,
      workshopUnits: 2,
      toolUnits: 3,
    });
  });

  it('should show an empty state without an open session', async () => {
    openSession.set(null);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('schbar-user-open-session')).toBeNull();
    expect(fixture.nativeElement.querySelector('schbar-empty-state')).not.toBeNull();
  });
});
