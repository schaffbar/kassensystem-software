import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { MatTabGroup } from '@angular/material/tabs';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { AgeGroup } from '../../../shared/models/age-group';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { WorkshopSessionStatus } from '../../../shared/models/workshop-session-status';
import { UserSessionBannerComponent } from '../../components/user-session-banner/user-session-banner.component';
import { RfidTagAssignment } from '../../rfid-tag-assignment.model';
import { User } from '../../user.model';
import { WorkshopSession } from '../../workshop-session.model';
import { UserDetailDialogsService } from './user-detail-dialogs.service';
import { UserDetailComponent } from './user-detail.component';
import { UserDetailStore } from './user-detail.store';

describe('UserDetailComponent', () => {
  const user: User = {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1990, 0, 31),
    ageGroup: AgeGroup.Adult,
    clubMember: true,
    email: 'max@example.org',
    phone: null,
    address: {
      addressLine1: 'Hauptstraße 1',
      addressLine2: null,
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    },
    createdAt: new Date(2026, 0, 1),
    updatedAt: new Date(2026, 0, 1),
  };
  const assignment: RfidTagAssignment = {
    id: 'a-1',
    customerId: 'user-1',
    rfidTagId: '04A31F9C',
    assignmentType: RfidTagAssignmentType.Fixed,
    status: RfidTagAssignmentStatus.Assigned,
    assignmentDate: new Date(2026, 1, 3),
  };
  // checked in at 14:05, used the band saw until 14:31, checked out at 16:30
  const session: WorkshopSession = {
    id: 'session-1',
    customerId: 'user-1',
    startTime: new Date(2026, 10, 9, 14, 5),
    closeTime: null,
    status: WorkshopSessionStatus.Open,
    workshopUsages: [
      {
        id: 'w-1',
        entryTime: new Date(2026, 10, 9, 14, 5),
        exitTime: new Date(2026, 10, 9, 16, 30),
        durationInMinutes: 145,
        unitsUsed: 3,
      },
    ],
    totalWorkshopDurationInMinutes: 145,
    totalWorkshopUnits: 3,
    toolUsageSummaries: [
      {
        toolId: 'tool-1',
        toolName: 'Bandsäge',
        active: false,
        totalDurationInMinutes: 26,
        totalUnits: 1,
        usages: [
          {
            id: 't-1',
            startTime: new Date(2026, 10, 9, 14, 5),
            endTime: new Date(2026, 10, 9, 14, 31),
            durationInMinutes: 26,
            unitsUsed: 1,
          },
        ],
      },
    ],
  };

  let fixture: ComponentFixture<UserDetailComponent>;
  let storeMock: {
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    user: WritableSignal<User | null>;
    rfidTagAssignment: WritableSignal<RfidTagAssignment | null>;
    openSession: WritableSignal<WorkshopSession | null>;
    canCloseWorkshopSession: WritableSignal<boolean>;
    canDeleteUser: WritableSignal<boolean>;
    isWaitingForRfidTag: WritableSignal<boolean>;
    toolCertifications: WritableSignal<[]>;
    tools: WritableSignal<[]>;
    userNames: WritableSignal<Map<string, string>>;
    setUserId: Mock;
    pollRfidTagAssignment: Mock;
    reloadUser: Mock;
  };
  let dialogsMock: { editUser: Mock; assignRfidTag: Mock; closeWorkshopSession: Mock; deleteUser: Mock };

  async function create(tab?: string): Promise<void> {
    fixture = TestBed.createComponent(UserDetailComponent);
    fixture.componentRef.setInput('id', 'user-1');
    if (tab) fixture.componentRef.setInput('tab', tab);
    await fixture.whenStable();
  }

  beforeEach(async () => {
    storeMock = {
      isLoading: signal(false),
      hasLoadError: signal(false),
      user: signal(user),
      rfidTagAssignment: signal(assignment),
      openSession: signal(null),
      canCloseWorkshopSession: signal(false),
      canDeleteUser: signal(true),
      isWaitingForRfidTag: signal(false),
      toolCertifications: signal([]),
      tools: signal([]),
      userNames: signal(new Map()),
      setUserId: vi.fn(),
      pollRfidTagAssignment: vi.fn(),
      reloadUser: vi.fn(),
    };
    dialogsMock = { editUser: vi.fn(), assignRfidTag: vi.fn(), closeWorkshopSession: vi.fn(), deleteUser: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [UserDetailComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService(), provideRouter([])],
    })
      .overrideComponent(UserDetailComponent, {
        set: {
          providers: [
            { provide: UserDetailStore, useValue: storeMock },
            { provide: UserDetailDialogsService, useValue: dialogsMock },
          ],
        },
      })
      .compileComponents();
  });

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  function query<T extends HTMLElement = HTMLElement>(selector: string): T | null {
    return fixture.nativeElement.querySelector(selector);
  }

  function texts(selector: string): string[] {
    return [...fixture.nativeElement.querySelectorAll(selector)].map((element: HTMLElement) =>
      element.textContent!.replace(/\s+/g, ' ').trim(),
    );
  }

  async function openMenu(): Promise<string[]> {
    query('schbar-row-menu button')?.click();
    await fixture.whenStable();
    return [...document.querySelectorAll('.mat-mdc-menu-item')].map((item) => item.textContent!.trim());
  }

  it('should load the user and follow a requested tag', async () => {
    await create();

    expect(storeMock.setUserId).toHaveBeenCalled();
    expect(storeMock.pollRfidTagAssignment).toHaveBeenCalledWith(storeMock.isWaitingForRfidTag);
  });

  it('should show name, initials and the badges in the header', async () => {
    await create();

    expect(query('h1')?.textContent).toBe('Max Mustermann');
    expect(query('.avatar')?.textContent).toBe('MM');
    expect(texts('schbar-status-badge')).toEqual([
      'users.chips.userDetails.ageGroup.adult',
      'verified users.chips.userDetails.clubMember',
      'sell users.chips.userDetails.rfidTags.fixed · 04A3 1F9C',
    ]);
    expect(query('.registered')?.textContent?.trim()).toBe('users.pages.userDetails.registeredOn');
  });

  it('should show a waiting tag as warning and offer no tag while a tag is assigned', async () => {
    storeMock.rfidTagAssignment.set({ ...assignment, status: RfidTagAssignmentStatus.WaitingForAssignment });
    await create();

    expect(texts('schbar-status-badge.tone-warn')).toEqual(['hourglass_top users.pages.userList.rfidTag.waiting']);
    expect(await openMenu()).not.toContain('sellusers.menu.assignRfidTag');
  });

  it('should open the dialogs from the header and its menu', async () => {
    storeMock.rfidTagAssignment.set(null);
    storeMock.canCloseWorkshopSession.set(true);
    await create();

    query<HTMLButtonElement>('schbar-page-header .actions > button')?.click();
    expect(dialogsMock.editUser).toHaveBeenCalledTimes(1);

    expect(await openMenu()).toEqual([
      'editusers.menu.edit',
      'sellusers.menu.assignRfidTag',
      'logoutusers.menu.closeSession',
      'deleteusers.menu.delete',
    ]);
    const items = document.querySelectorAll<HTMLElement>('.mat-mdc-menu-item');
    items[1].click();
    items[2].click();
    items[3].click();
    expect(dialogsMock.assignRfidTag).toHaveBeenCalledTimes(1);
    expect(dialogsMock.closeWorkshopSession).toHaveBeenCalledTimes(1);
    expect(dialogsMock.deleteUser).toHaveBeenCalledTimes(1);
  });

  it('should not offer to delete a user with an open session', async () => {
    storeMock.canDeleteUser.set(false);
    await create();

    expect(await openMenu()).toEqual(['editusers.menu.edit']);
  });

  it('should show the banner only with an open session and close it from there', async () => {
    await create();
    expect(query('schbar-user-session-banner')).toBeNull();

    storeMock.openSession.set(session);
    await fixture.whenStable();
    const banner: UserSessionBannerComponent = fixture.debugElement.query(
      By.directive(UserSessionBannerComponent),
    ).componentInstance;
    expect(banner.userSessionBanner()).toEqual({
      startTime: session.startTime,
      inWorkshop: false,
      checkedOutAt: new Date(2026, 10, 9, 16, 30),
      canClose: true,
    });

    banner.closeRequested.emit();
    expect(dialogsMock.closeWorkshopSession).toHaveBeenCalledTimes(1);
  });

  it('should lock closing while the user is in the workshop', async () => {
    storeMock.openSession.set({
      ...session,
      workshopUsages: [{ ...session.workshopUsages[0], exitTime: null, durationInMinutes: null, unitsUsed: null }],
    });
    await create();

    const banner: UserSessionBannerComponent = fixture.debugElement.query(
      By.directive(UserSessionBannerComponent),
    ).componentInstance;
    expect(banner.userSessionBanner()).toEqual({
      startTime: session.startTime,
      inWorkshop: true,
      checkedOutAt: null,
      canClose: false,
    });
  });

  it('should open the session tab from the route', async () => {
    await create('open-session');

    expect(fixture.debugElement.query(By.directive(MatTabGroup)).componentInstance.selectedIndex).toBe(2);
  });

  it('should reload everything', async () => {
    await create();

    query<HTMLButtonElement>('button[aria-label="shared.actions.reload"]')?.click();

    expect(storeMock.reloadUser).toHaveBeenCalledTimes(1);
  });
});
