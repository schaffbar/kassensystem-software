import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MatDialog } from '@angular/material/dialog';

import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CloseSessionDialogService } from '../../../shared/dialogs/close-session-dialog/close-session-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { AgeGroup } from '../../../shared/models/age-group';
import { AssignRfidTagDialogComponent } from '../../components/assign-rfid-tag-dialog/assign-rfid-tag-dialog.component';
import { UpdateUserFormComponent } from '../../components/update-user-form/update-user-form.component';
import { UpdateUserCommand, User } from '../../user.model';
import { UserDetailDialogsService } from './user-detail-dialogs.service';
import { UserDetailStore } from './user-detail.store';

describe('UserDetailDialogsService', () => {
  const user: User = {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1990, 0, 31),
    ageGroup: AgeGroup.Adult,
    clubMember: false,
    email: 'max@example.org',
    phone: null,
    address: { addressLine1: 'Hauptstraße 1', addressLine2: null, postalCode: '12345', city: 'Berlin', country: 'D' },
    createdAt: new Date(2026, 0, 1),
    updatedAt: new Date(2026, 0, 1),
  };

  let service: UserDetailDialogsService;
  let storeMock: {
    user: WritableSignal<User | null>;
    rfidTagAssignment: WritableSignal<null>;
    updateUser: Mock;
    assignRfidTag: Mock & { isPending: WritableSignal<boolean> };
    closeWorkshopSession: Mock;
    reloadOpenSession: Mock;
    deleteUser: Mock;
  };
  let openDialog: Mock;
  let openCloseSession: Mock;
  let openConfirmDelete: Mock;

  beforeEach(() => {
    storeMock = {
      user: signal<User | null>(user),
      rfidTagAssignment: signal(null),
      updateUser: vi.fn(),
      assignRfidTag: Object.assign(vi.fn(), { isPending: signal(false) }),
      closeWorkshopSession: vi.fn(),
      reloadOpenSession: vi.fn(),
      deleteUser: vi.fn(),
    };
    openDialog = vi.fn();
    openCloseSession = vi.fn();
    openConfirmDelete = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        UserDetailDialogsService,
        { provide: UserDetailStore, useValue: storeMock },
        { provide: MatDialog, useValue: { open: openDialog } },
        { provide: CloseSessionDialogService, useValue: { open: openCloseSession } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDelete } },
      ],
    });
    service = TestBed.inject(UserDetailDialogsService);
  });

  it('should edit the user in the form dialog and save the command', () => {
    const command = { userId: 'user-1', firstName: 'Moritz' } as UpdateUserCommand;
    openDialog.mockReturnValue({ afterClosed: () => of(command) });

    service.editUser();

    expect(openDialog).toHaveBeenCalledWith(
      UpdateUserFormComponent,
      expect.objectContaining({ ...FORM_DIALOG_CONFIG, data: { user } }),
    );
    expect(storeMock.updateUser).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('should save nothing when editing is cancelled', () => {
    openDialog.mockReturnValue({ afterClosed: () => of(undefined) });

    service.editUser();

    expect(storeMock.updateUser).not.toHaveBeenCalled();
  });

  it('should open the tag dialog bound to the assignment of the store', () => {
    service.assignRfidTag();

    expect(openDialog).toHaveBeenCalledWith(
      AssignRfidTagDialogComponent,
      expect.objectContaining({ ...FORM_DIALOG_CONFIG, bindings: expect.any(Array) }),
    );
    expect(openDialog.mock.calls[0][1].bindings).toHaveLength(5);
  });

  it('should close the session after confirmation', () => {
    openCloseSession.mockReturnValue(of(true));

    service.closeWorkshopSession();

    expect(openCloseSession).toHaveBeenCalledWith('user-1', 'Max Mustermann');
    expect(storeMock.closeWorkshopSession).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
  });

  it('should reload the session when closing is cancelled – the dialog may have shown a newer state', () => {
    openCloseSession.mockReturnValue(of(false));

    service.closeWorkshopSession();

    expect(storeMock.closeWorkshopSession).not.toHaveBeenCalled();
    expect(storeMock.reloadOpenSession).toHaveBeenCalledTimes(1);
  });

  it('should delete the user after confirmation', () => {
    openConfirmDelete.mockReturnValue(of(true));

    service.deleteUser();

    expect(openConfirmDelete).toHaveBeenCalledWith(
      expect.objectContaining({ entity: 'Max Mustermann', confirmLabel: 'users.dialogs.deleteUser.confirm' }),
    );
    expect(storeMock.deleteUser).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
  });

  it('should not delete the user without confirmation', () => {
    openConfirmDelete.mockReturnValue(of(false));

    service.deleteUser();

    expect(storeMock.deleteUser).not.toHaveBeenCalled();
  });

  it('should open nothing before the user is loaded', () => {
    storeMock.user.set(null);

    service.editUser();
    service.assignRfidTag();
    service.closeWorkshopSession();
    service.deleteUser();

    expect(openDialog).not.toHaveBeenCalled();
    expect(openCloseSession).not.toHaveBeenCalled();
    expect(openConfirmDelete).not.toHaveBeenCalled();
  });
});
