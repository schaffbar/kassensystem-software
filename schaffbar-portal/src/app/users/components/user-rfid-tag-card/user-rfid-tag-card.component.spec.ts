import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { UserRfidTagCardComponent, UserRfidTagCardViewModel } from './user-rfid-tag-card.component';

describe('UserRfidTagCardComponent', () => {
  const assigned: UserRfidTagCardViewModel = {
    userId: 'user-1',
    userName: 'Max Mustermann',
    assignment: {
      assignmentType: RfidTagAssignmentType.Fixed,
      status: RfidTagAssignmentStatus.Assigned,
      rfidTagId: '04A31F9C',
      assignmentDate: new Date(2025, 1, 3),
    },
  };

  let fixture: ComponentFixture<UserRfidTagCardComponent>;
  let openConfirm: Mock;

  beforeEach(() => {
    openConfirm = vi.fn();
    TestBed.configureTestingModule({
      imports: [UserRfidTagCardComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ConfirmActionDialogService, useValue: { open: openConfirm } },
      ],
    });
    fixture = TestBed.createComponent(UserRfidTagCardComponent);
  });

  async function render(viewModel: UserRfidTagCardViewModel): Promise<void> {
    fixture.componentRef.setInput('userRfidTagCard', viewModel);
    await fixture.whenStable();
  }

  function text(selector: string): string | undefined {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim();
  }

  function clickButton(): void {
    fixture.nativeElement.querySelector('.actions button').click();
  }

  it('should show the assigned tag and unassign it after confirmation', async () => {
    await render(assigned);
    openConfirm.mockReturnValue(of(true));
    const rfidTagUnassigned = vi.fn();
    fixture.componentInstance.rfidTagUnassigned.subscribe(rfidTagUnassigned);

    expect(text('.tag-id')).toBe('04A3 1F9C');
    expect(text('.tag-detail')).toBe(
      'users.chips.userDetails.rfidTags.fixed · users.pages.userDetails.rfidTag.assignedOn',
    );
    clickButton();

    expect(openConfirm).toHaveBeenCalledWith(expect.objectContaining({ entity: 'Max Mustermann', destructive: true }));
    expect(rfidTagUnassigned).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
  });

  it('should not unassign without confirmation', async () => {
    await render(assigned);
    openConfirm.mockReturnValue(of(false));
    const rfidTagUnassigned = vi.fn();
    fixture.componentInstance.rfidTagUnassigned.subscribe(rfidTagUnassigned);

    clickButton();

    expect(rfidTagUnassigned).not.toHaveBeenCalled();
  });

  it('should show a waiting request and open it again', async () => {
    await render({
      ...assigned,
      assignment: { ...assigned.assignment!, status: RfidTagAssignmentStatus.WaitingForAssignment, rfidTagId: null },
    });
    const assignmentRequested = vi.fn();
    fixture.componentInstance.assignmentRequested.subscribe(assignmentRequested);

    expect(text('.tag-title')).toBe('users.pages.userList.rfidTag.waiting');
    clickButton();

    expect(assignmentRequested).toHaveBeenCalledTimes(1);
  });

  it('should offer to assign a tag when there is none', async () => {
    await render({ ...assigned, assignment: null });
    const assignmentRequested = vi.fn();
    fixture.componentInstance.assignmentRequested.subscribe(assignmentRequested);

    expect(text('.no-tag')).toBe('users.pages.userDetails.rfidTag.none');
    clickButton();

    expect(assignmentRequested).toHaveBeenCalledTimes(1);
  });
});
