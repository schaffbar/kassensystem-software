import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { AgeGroup } from '../../../shared/models/age-group';
import { ToolArea } from '../../../shared/models/tool-area';
import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { ActiveUsersCardComponent } from '../../components/active-users-card/active-users-card.component';
import { DashboardAttentionComponent } from '../../components/dashboard-attention/dashboard-attention.component';
import { StatTileComponent } from '../../components/stat-tile/stat-tile.component';
import { ToolStatusCardComponent } from '../../components/tool-status-card/tool-status-card.component';
import { ActiveTool, ActiveUser, RfidReader, Tool, User } from '../../dashboard.model';
import { DashboardStore } from '../../dashboard.store';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  const bandsaw: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
    area: ToolArea.Holz,
    rfidReaderId: 'reader-1',
    wlanRelaisType: WlanRelaisType.Shelly1,
  };
  const lathe: Tool = {
    id: 'tool-2',
    name: 'Drechselbank',
    area: ToolArea.Holz,
    rfidReaderId: null,
    wlanRelaisType: null,
  };
  const activeTools: ActiveTool[] = [
    { toolId: 'tool-1', toolName: 'Bandsäge', customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann' },
  ];
  const activeUsers: ActiveUser[] = [
    { customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann', entryTime: new Date(2026, 10, 11, 14, 5) },
    { customerId: 'user-2', firstName: 'Lena', lastName: 'Klein', entryTime: new Date(2026, 10, 11, 15, 0) },
  ];
  const users: User[] = [
    { id: 'user-1', ageGroup: AgeGroup.Adult, clubMember: true },
    { id: 'user-2', ageGroup: AgeGroup.Under16, clubMember: false },
  ];
  const newReader: RfidReader = { id: 'reader-2', name: null, macAddress: 'CC:DD', type: null };

  let fixture: ComponentFixture<DashboardComponent>;
  let storeMock: {
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    updatedAt: WritableSignal<Date | null>;
    tools: WritableSignal<Tool[]>;
    activeTools: WritableSignal<ActiveTool[]>;
    activeUsers: WritableSignal<ActiveUser[]>;
    users: WritableSignal<User[]>;
    minorsInWorkshopCount: WritableSignal<number>;
    toolsWithoutHardware: WritableSignal<Tool[]>;
    rfidReadersWithoutType: WritableSignal<RfidReader[]>;
    reloadDashboard: Mock;
    pollDashboard: Mock;
  };

  beforeEach(async () => {
    storeMock = {
      isLoading: signal(false),
      hasLoadError: signal(false),
      updatedAt: signal(new Date(2026, 10, 11, 15, 30)),
      tools: signal([bandsaw, lathe]),
      activeTools: signal(activeTools),
      activeUsers: signal(activeUsers),
      users: signal(users),
      minorsInWorkshopCount: signal(1),
      toolsWithoutHardware: signal([lathe]),
      rfidReadersWithoutType: signal([newReader]),
      reloadDashboard: vi.fn(),
      pollDashboard: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideTranslateService()],
    })
      .overrideComponent(DashboardComponent, { set: { providers: [{ provide: DashboardStore, useValue: storeMock }] } })
      .compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    await fixture.whenStable();
  });

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should reload itself every minute and on the button', () => {
    expect(storeMock.pollDashboard).toHaveBeenCalledExactlyOnceWith(60_000);

    fixture.nativeElement.querySelector('button[aria-label="shared.actions.reload"]').click();

    expect(storeMock.reloadDashboard).toHaveBeenCalledOnce();
  });

  it('should link what needs attention: one entity directly, several to their list', async () => {
    expect(child(DashboardAttentionComponent).items()).toEqual([
      { kind: 'rfidReadersWithoutType', names: ['CC:DD'], link: ['/', 'rfid-readers', 'reader-2'] },
      { kind: 'toolsWithoutHardware', names: ['Drechselbank'], link: ['/', 'tools', 'tool-2'] },
    ]);

    storeMock.rfidReadersWithoutType.set([]);
    storeMock.toolsWithoutHardware.set([lathe, { ...lathe, id: 'tool-3', name: 'Hobel' }]);
    await fixture.whenStable();

    expect(child(DashboardAttentionComponent).items()).toEqual([
      { kind: 'toolsWithoutHardware', names: ['Drechselbank', 'Hobel'], link: ['/', 'tools'] },
    ]);
  });

  it('should hide the attention block when everything is set up', async () => {
    storeMock.rfidReadersWithoutType.set([]);
    storeMock.toolsWithoutHardware.set([]);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('schbar-dashboard-attention')).toBeNull();
  });

  it('should show the people in the workshop and the tools in use as key figures', () => {
    const tiles = fixture.debugElement.queryAll(By.directive(StatTileComponent)).map((tile) => tile.componentInstance);

    expect(tiles.map((tile: StatTileComponent) => [tile.label(), tile.value(), tile.total(), tile.detail()])).toEqual([
      ['dashboard.stats.inWorkshop', 2, null, 'dashboard.stats.minors'],
      ['dashboard.stats.toolsInUse', 1, 2, 'Bandsäge'],
    ]);
  });

  it('should join age group, membership and tool into the people in the workshop', () => {
    expect(child(ActiveUsersCardComponent).activeUsers()).toEqual([
      { ...activeUsers[0], ageGroup: AgeGroup.Adult, clubMember: true, toolName: 'Bandsäge' },
      { ...activeUsers[1], ageGroup: AgeGroup.Under16, clubMember: false, toolName: null },
    ]);
  });

  it('should pass the tools with the person using them and the areas with their counts', () => {
    const card = child(ToolStatusCardComponent);

    expect(card.toolStatuses()).toEqual([
      { toolId: 'tool-1', name: 'Bandsäge', area: ToolArea.Holz, usedBy: 'Max Mustermann' },
      { toolId: 'tool-2', name: 'Drechselbank', area: ToolArea.Holz, usedBy: null },
    ]);
    expect(card.areaSegments().map((segment) => [segment.id, segment.count])).toEqual([
      ['ALL', 2],
      [ToolArea.Holz, 2],
      [ToolArea.Metall, 0],
      [ToolArea.Kreativ, 0],
      [ToolArea.FabLab, 0],
    ]);
  });
});
