import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSnackBar } from '@angular/material/snack-bar';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { AgeGroup } from '../../../shared/models/age-group';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { NewUserFormComponent } from '../../components/new-user-form/new-user-form.component';
import { CreateUserCommand, UserListItem } from '../../user.model';
import { UsersStore } from '../../users.store';
import { UserListComponent } from './user-list.component';

describe('UserListComponent', () => {
  const user = (overrides: Partial<UserListItem>): UserListItem => ({
    id: 'user',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1990, 0, 31),
    ageGroup: AgeGroup.Adult,
    clubMember: false,
    email: 'max@example.org',
    phone: null,
    address: { addressLine1: 'Hauptstraße 1', addressLine2: null, postalCode: '12345', city: 'Berlin', country: 'D' },
    createdAt: new Date('2026-01-01T10:00:00Z'),
    updatedAt: new Date('2026-01-01T10:00:00Z'),
    rfidTag: null,
    inWorkshop: false,
    ...overrides,
  });
  const users: UserListItem[] = [
    user({
      id: 'user-max',
      clubMember: true,
      rfidTag: { assignmentType: RfidTagAssignmentType.Fixed, rfidTagId: '04A31F9C' },
      inWorkshop: true,
      createdAt: new Date('2026-01-01T10:00:00Z'),
    }),
    user({
      id: 'user-erika',
      firstName: 'Erika',
      lastName: 'Becker',
      email: 'erika@example.org',
      ageGroup: AgeGroup.Under16,
      rfidTag: { assignmentType: RfidTagAssignmentType.Temporary, rfidTagId: null },
      createdAt: new Date('2026-03-01T10:00:00Z'),
    }),
    user({
      id: 'user-otto',
      firstName: 'Otto',
      lastName: 'Zander',
      email: 'otto@example.org',
      ageGroup: AgeGroup.Under18,
      rfidTag: { assignmentType: RfidTagAssignmentType.Temporary, rfidTagId: '04B7720E' },
      createdAt: new Date('2026-02-01T10:00:00Z'),
    }),
  ];

  let fixture: ComponentFixture<UserListComponent>;
  let loader: HarnessLoader;
  let storeMock: {
    users: WritableSignal<UserListItem[]>;
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    reloadUsers: Mock;
    canDeleteUser: Mock;
    createUser: Mock & { isPending: WritableSignal<boolean> };
    deleteUser: Mock;
  };
  let openDialog: Mock;
  let openConfirmDeleteDialog: Mock;
  let openSnackBar: Mock;

  beforeEach(async () => {
    storeMock = {
      users: signal(users),
      isLoading: signal(false),
      hasLoadError: signal(false),
      reloadUsers: vi.fn(),
      canDeleteUser: vi.fn(),
      createUser: Object.assign(vi.fn(), { isPending: signal(false) }),
      deleteUser: vi.fn(),
    };
    openDialog = vi.fn();
    openConfirmDeleteDialog = vi.fn();
    openSnackBar = vi.fn();

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: UsersStore, useValue: storeMock },
        { provide: MatDialog, useValue: { open: openDialog } },
        { provide: MatSnackBar, useValue: { open: openSnackBar } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDeleteDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  function texts(selector: string): string[] {
    const elements: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll(selector));
    return elements.map((element) => element.textContent!.replace(/\s+/g, ' ').trim());
  }

  const names = (): string[] => texts('.user-name').map((name) => name.replace(' users.pages.userList.member', ''));

  async function search(searchText: string): Promise<void> {
    await (await loader.getHarness(MatInputHarness.with({ selector: '[type="search"]' }))).setValue(searchText);
    await fixture.whenStable();
  }

  async function selectFilter(id: string): Promise<void> {
    const chips: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('schbar-filter-chips button'));
    chips.find((chip) => chip.textContent!.includes(`users.pages.userList.filters.${id}`))!.click();
    await fixture.whenStable();
  }

  async function openRowMenu(row: number): Promise<HTMLElement[]> {
    fixture.nativeElement.querySelectorAll('tr[mat-row] schbar-row-menu button')[row].click();
    await fixture.whenStable();
    return Array.from(document.querySelectorAll<HTMLElement>('[mat-menu-item]'));
  }

  it('should load the users on init', () => {
    expect(storeMock.reloadUsers).toHaveBeenCalledOnce();
  });

  it('should list the users by last name with membership, e-mail, age group, tag and presence', () => {
    expect(names()).toEqual(['Erika Becker', 'Max Mustermann', 'Otto Zander']);
    expect(texts('.user-email')).toEqual(['erika@example.org', 'max@example.org', 'otto@example.org']);
    expect(texts('.member-badge')).toEqual(['users.pages.userList.member']);
    expect(texts('.age-group')).toEqual([
      'shared.ageGroups.under_16',
      'shared.ageGroups.adult',
      'shared.ageGroups.under_18',
    ]);
    expect(texts('.rfid-tag')).toEqual([
      'hourglass_top users.pages.userList.rfidTag.waiting',
      'users.chips.userDetails.rfidTags.fixed · 04A3 1F9C',
      'users.chips.userDetails.rfidTags.temporary · 04B7 720E',
    ]);
    expect(texts('schbar-status-badge')).toEqual(['login users.pages.userList.inWorkshop']);
  });

  it('should show a dash for a user without tag – that is normal outside the workshop', async () => {
    storeMock.users.set([user({ rfidTag: null })]);
    await fixture.whenStable();

    expect(texts('.rfid-tag')).toEqual(['–']);
  });

  it('should count the users of each filter', () => {
    expect(texts('schbar-filter-chips button')).toEqual([
      'users.pages.userList.filters.all 3',
      'users.pages.userList.filters.inWorkshop 1',
      'users.pages.userList.filters.clubMembers 1',
      'users.pages.userList.filters.temporaryRfidTag 1',
    ]);
  });

  it('should filter the users', async () => {
    await selectFilter('inWorkshop');
    expect(names()).toEqual(['Max Mustermann']);

    await selectFilter('clubMembers');
    expect(names()).toEqual(['Max Mustermann']);

    // a loan tag is only counted once it is handed out, not while the user waits for it
    await selectFilter('temporaryRfidTag');
    expect(names()).toEqual(['Otto Zander']);
  });

  it('should search name, e-mail and tag ID with or without spaces', async () => {
    await search('ZAND');
    expect(names()).toEqual(['Otto Zander']);

    await search('erika@');
    expect(names()).toEqual(['Erika Becker']);

    await search('04a3 1f');
    expect(names()).toEqual(['Max Mustermann']);
  });

  it('should say so when nothing matches', async () => {
    await search('nobody');

    expect(fixture.nativeElement.querySelector('schbar-empty-state.no-results')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.table-card').hidden).toBe(true);
  });

  it('should show an empty state without users', async () => {
    storeMock.users.set([]);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('schbar-empty-state.table-card')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });

  it('should open the profile from the row and from the row menu', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.nativeElement.querySelector('tr[mat-row]').click();
    (await openRowMenu(1))[0].click();

    expect(navigate.mock.calls).toEqual([[['users', 'user-erika']], [['users', 'user-max']]]);
  });

  describe('create', () => {
    const command: CreateUserCommand = {
      firstName: 'Paul',
      lastName: 'Panzer',
      dateOfBirth: new Date(1990, 0, 31),
      clubMember: false,
      email: 'paul@example.org',
      addressLine1: 'Hauptstraße 1',
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    };

    function addButton(): HTMLButtonElement {
      return fixture.nativeElement.querySelector('schbar-page-header button[matButton]');
    }

    it('should open the form with the users for the duplicate hint and create the user', () => {
      openDialog.mockReturnValue({ afterClosed: () => of(command) });

      addButton().click();

      expect(openDialog).toHaveBeenCalledExactlyOnceWith(
        NewUserFormComponent,
        expect.objectContaining({ ...FORM_DIALOG_CONFIG, data: { existingUsers: storeMock.users } }),
      );
      expect(storeMock.createUser).toHaveBeenCalledExactlyOnceWith(command);
    });

    it('should disable adding while a user is being created', async () => {
      storeMock.createUser.isPending.set(true);
      await fixture.whenStable();

      expect(addButton().disabled).toBe(true);
    });

    it('should not create a user when the form is cancelled', () => {
      openDialog.mockReturnValue({ afterClosed: () => of(undefined) });

      addButton().click();

      expect(storeMock.createUser).not.toHaveBeenCalled();
    });
  });

  describe('delete', () => {
    async function deleteFirstRow(): Promise<void> {
      const items = await openRowMenu(0);
      items.find((item) => item.classList.contains('danger-menu-item'))!.click();
      await fixture.whenStable();
    }

    it('should delete the user after confirmation', async () => {
      storeMock.canDeleteUser.mockReturnValue(of(true));
      openConfirmDeleteDialog.mockReturnValue(of(true));

      await deleteFirstRow();

      expect(storeMock.canDeleteUser).toHaveBeenCalledExactlyOnceWith('user-erika');
      expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'users.dialogs.deleteUser.title',
        entity: 'Erika Becker',
        message: 'users.dialogs.deleteUser.message',
        confirmLabel: 'users.dialogs.deleteUser.confirm',
      });
      expect(storeMock.deleteUser).toHaveBeenCalledExactlyOnceWith({ userId: 'user-erika' });
    });

    it('should not delete the user when the dialog is cancelled', async () => {
      storeMock.canDeleteUser.mockReturnValue(of(true));
      openConfirmDeleteDialog.mockReturnValue(of(false));

      await deleteFirstRow();

      expect(storeMock.deleteUser).not.toHaveBeenCalled();
    });

    it('should explain instead of asking when the user has an open session', async () => {
      storeMock.canDeleteUser.mockReturnValue(of(false));

      await deleteFirstRow();

      expect(openConfirmDeleteDialog).not.toHaveBeenCalled();
      expect(openSnackBar).toHaveBeenCalledExactlyOnceWith(
        'users.dialogs.deleteUser.openSession',
        'shared.actions.close',
        expect.anything(),
      );
    });
  });
});
