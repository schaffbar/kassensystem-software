import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AgeGroup } from '../../../shared/models/age-group';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import {
  RfidTagAssignmentViewModel,
  UserDetailHeaderComponent,
  UserDetailHeaderViewModel,
} from './user-detail-header.component';

describe('UserDetailHeaderComponent', () => {
  const userDetailHeader: UserDetailHeaderViewModel = {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    ageGroup: AgeGroup.Adult,
    clubMember: true,
  };
  const assigned: RfidTagAssignmentViewModel = {
    assignmentType: RfidTagAssignmentType.Fixed,
    status: RfidTagAssignmentStatus.Assigned,
    rfidTagId: 'tag-42',
  };

  let fixture: ComponentFixture<UserDetailHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserDetailHeaderComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailHeaderComponent);
    fixture.componentRef.setInput('userDetailHeader', userDetailHeader);
    fixture.componentRef.setInput('rfidTagAssignment', null);
    await fixture.whenStable();
  });

  async function render(rfidTagAssignment: RfidTagAssignmentViewModel | null): Promise<void> {
    fixture.componentRef.setInput('rfidTagAssignment', rfidTagAssignment);
    await fixture.whenStable();
  }

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.trim() ?? '');
  }

  function actionButtons(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.user-detail-header-actions button'));
  }

  it('should show the name and the chips of a user without RFID tag', () => {
    expect(texts('h1')).toEqual(['Max Mustermann']);
    expect(texts('mat-chip')).toEqual([
      'users.chips.userDetails.ageGroup.adult',
      'users.chips.userDetails.clubMember',
      'users.chips.userDetails.rfidTags.noRfidTag',
    ]);
  });

  it('should emit an assign command per assignment type', () => {
    const assign = vi.fn();
    fixture.componentInstance.rfidTagAssigned.subscribe(assign);

    const [temporary, fixed] = actionButtons();
    temporary.click();
    fixed.click();

    expect(assign.mock.calls).toEqual([
      [{ userId: 'user-1', assignmentType: RfidTagAssignmentType.Temporary }],
      [{ userId: 'user-1', assignmentType: RfidTagAssignmentType.Fixed }],
    ]);
  });

  it('should show the waiting state without actions', async () => {
    await render({ ...assigned, status: RfidTagAssignmentStatus.WaitingForAssignment, rfidTagId: null });

    expect(texts('mat-chip').slice(2)).toEqual([
      'users.chips.userDetails.rfidTags.fixed',
      'users.chips.userDetails.rfidTags.waiting',
    ]);
    expect(actionButtons()).toEqual([]);
  });

  it('should show the assigned tag and emit the unassign command', async () => {
    await render(assigned);
    const unassign = vi.fn();
    fixture.componentInstance.rfidTagUnassigned.subscribe(unassign);

    expect(texts('mat-chip').slice(2)).toEqual(['users.chips.userDetails.rfidTags.fixed', 'tag-42']);
    actionButtons()[0].click();

    expect(unassign).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
  });

  it('should emit edit and reload', () => {
    const edit = vi.fn();
    const reload = vi.fn();
    fixture.componentInstance.editRequested.subscribe(edit);
    fixture.componentInstance.reloadRequested.subscribe(reload);

    fixture.nativeElement.querySelector('[aria-label="Edit user"]').click();
    fixture.nativeElement.querySelector('[aria-label="Reload user"]').click();

    expect(edit).toHaveBeenCalledOnce();
    expect(reload).toHaveBeenCalledOnce();
  });
});
