import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { NewUserFormComponent } from '../../components/new-user-form/new-user-form.component';
import { CreateUserCommand, User } from '../../user.model';
import { UsersStore } from '../../users.store';
import { UserListComponent } from './user-list.component';

describe('UserListComponent', () => {
  const users = [
    { id: 'user-1', firstName: 'Max', lastName: 'Mustermann' },
    { id: 'user-2', firstName: 'Erika', lastName: 'Musterfrau' },
    { id: 'user-3', firstName: 'Otto', lastName: 'Normalverbraucher' },
  ] as User[];

  let fixture: ComponentFixture<UserListComponent>;
  let loader: HarnessLoader;
  let storeUsers: WritableSignal<User[]>;
  let createUser: Mock & { isPending: WritableSignal<boolean> };
  let deleteUser: Mock;
  let openDialog: Mock;
  let openConfirmDeleteDialog: Mock;

  beforeEach(async () => {
    storeUsers = signal(users);
    createUser = Object.assign(vi.fn(), { isPending: signal(false) });
    deleteUser = vi.fn();
    openDialog = vi.fn();
    openConfirmDeleteDialog = vi.fn();

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        {
          provide: UsersStore,
          useValue: {
            users: storeUsers,
            isLoading: signal(false),
            hasLoadError: signal(false),
            reloadUsers: vi.fn(),
            createUser,
            deleteUser,
          },
        },
        { provide: MatDialog, useValue: { open: openDialog } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDeleteDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  async function search(term: string): Promise<void> {
    await (await loader.getHarness(MatInputHarness.with({ selector: '#search' }))).setValue(term);
    await fixture.whenStable();
  }

  async function lastNames(): Promise<string[]> {
    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getCellTextByColumnName();
    return rows['lastName'].text;
  }

  it('should show all users without a search term', async () => {
    expect(await lastNames()).toEqual(['Mustermann', 'Musterfrau', 'Normalverbraucher']);
  });

  it('should filter the users by the search term (case-insensitive)', async () => {
    await search('MUSTER');

    expect(await lastNames()).toEqual(['Mustermann', 'Musterfrau']);
  });

  it('should keep the filter when the users are reloaded', async () => {
    await search('otto');

    storeUsers.set([...users, { id: 'user-4', firstName: 'Otto', lastName: 'Waalkes' } as User]);
    await fixture.whenStable();

    expect(await lastNames()).toEqual(['Normalverbraucher', 'Waalkes']);
  });

  it('should clear the search with the clear button', async () => {
    await search('max');

    const clear = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Clear search"]' }));
    await clear.click();
    await fixture.whenStable();

    expect(await (await loader.getHarness(MatInputHarness.with({ selector: '#search' }))).getValue()).toBe('');
    expect(await lastNames()).toHaveLength(3);
    expect(
      await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Clear search"]' })),
    ).toBeNull();
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

    function clickAdd(result: CreateUserCommand | undefined): void {
      openDialog.mockReturnValue({ afterClosed: () => of(result) });
      fixture.nativeElement.querySelector('.user-list-header__actions button').click();
    }

    it('should open the form and create the user', () => {
      clickAdd(command);

      expect(openDialog).toHaveBeenCalledExactlyOnceWith(
        NewUserFormComponent,
        expect.objectContaining({ disableClose: true }),
      );
      expect(createUser).toHaveBeenCalledExactlyOnceWith(command);
    });

    it('should disable adding while a user is being created', async () => {
      createUser.isPending.set(true);
      await fixture.whenStable();

      expect(fixture.nativeElement.querySelector('.user-list-header__actions button').disabled).toBe(true);
    });

    it('should not create a user when the form is cancelled', () => {
      clickAdd(undefined);

      expect(createUser).not.toHaveBeenCalled();
    });
  });

  describe('delete', () => {
    function clickDeleteOfFirstRow(confirmed: boolean): void {
      openConfirmDeleteDialog.mockReturnValue(of(confirmed));
      fixture.nativeElement.querySelector('tr[mat-row] .danger-button').click();
    }

    it('should delete the user after confirmation', () => {
      clickDeleteOfFirstRow(true);

      expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'users.dialogs.deleteUser.title',
        entity: 'Max Mustermann',
        message: 'users.dialogs.deleteUser.message',
      });
      expect(deleteUser).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
    });

    it('should not delete the user when the dialog is cancelled', () => {
      clickDeleteOfFirstRow(false);

      expect(deleteUser).not.toHaveBeenCalled();
    });
  });
});
