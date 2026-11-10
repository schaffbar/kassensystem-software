import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { AssignRfidTagDialogComponent, AssignRfidTagDialogViewModel } from './assign-rfid-tag-dialog.component';

describe('AssignRfidTagDialogComponent', () => {
  const waiting: AssignRfidTagDialogViewModel = {
    assignmentType: RfidTagAssignmentType.Fixed,
    status: RfidTagAssignmentStatus.WaitingForAssignment,
    rfidTagId: null,
  };
  const assigned: AssignRfidTagDialogViewModel = {
    ...waiting,
    status: RfidTagAssignmentStatus.Assigned,
    rfidTagId: '04C95A12',
  };

  let fixture: ComponentFixture<AssignRfidTagDialogComponent>;
  let loader: HarnessLoader;
  let assignmentRequested: Mock;

  beforeEach(async () => {
    assignmentRequested = vi.fn();
    await TestBed.configureTestingModule({
      imports: [AssignRfidTagDialogComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AssignRfidTagDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.componentRef.setInput('userId', 'user-1');
    fixture.componentRef.setInput('userName', 'Max Mustermann');
    fixture.componentRef.setInput('rfidTagAssignment', null);
    fixture.componentInstance.assignmentRequested.subscribe(assignmentRequested);
    await fixture.whenStable();
  });

  async function setAssignment(assignment: AssignRfidTagDialogViewModel | null, requestPending = false) {
    fixture.componentRef.setInput('rfidTagAssignment', assignment);
    fixture.componentRef.setInput('requestPending', requestPending);
    await fixture.whenStable();
  }

  function text(selector: string): string | undefined {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim();
  }

  function currentStep(): string | undefined {
    return text('.steps [aria-current="step"]');
  }

  async function requestTag(): Promise<void> {
    await (await loader.getHarness(MatButtonHarness.with({ text: 'users.dialogs.assignRfidTag.confirm' }))).click();
    await fixture.whenStable();
  }

  it('should start with the choice of a loan tag', async () => {
    const radioGroup = await loader.getHarness(MatRadioGroupHarness);

    expect(currentStep()).toBe('users.dialogs.assignRfidTag.steps.choose');
    expect(await radioGroup.getCheckedValue()).toBe(RfidTagAssignmentType.Temporary);
    expect(text('.radio-card-description')).toBe('users.dialogs.assignRfidTag.descriptions.TEMPORARY');
  });

  it('should request the chosen type of tag', async () => {
    await (
      await loader.getHarness(MatRadioGroupHarness)
    ).checkRadioButton({
      label: /users.chips.userDetails.rfidTags.fixed/,
    });

    await requestTag();

    expect(assignmentRequested).toHaveBeenCalledExactlyOnceWith({
      userId: 'user-1',
      assignmentType: RfidTagAssignmentType.Fixed,
    });
  });

  it('should wait for the tag after the request and show it when it has been held to the reader', async () => {
    await requestTag();

    await setAssignment(null, true);
    expect(currentStep()).toBe('users.dialogs.assignRfidTag.steps.wait');
    expect(text('.result-title')).toBe('users.dialogs.assignRfidTag.waiting.requesting');

    await setAssignment(waiting);
    expect(text('.result-title')).toBe('users.dialogs.assignRfidTag.waiting.title');

    await setAssignment(assigned);
    expect(currentStep()).toBe('users.dialogs.assignRfidTag.steps.done');
    expect(text('.result-title')).toBe('users.dialogs.assignRfidTag.done.title');
    expect(text('.tag-id')).toBe('04C9 5A12');
  });

  it('should continue a request that already waits', async () => {
    await setAssignment(waiting);
    expect(currentStep()).toBe('users.dialogs.assignRfidTag.steps.wait');

    await setAssignment(assigned);
    expect(currentStep()).toBe('users.dialogs.assignRfidTag.steps.done');
  });

  it('should go back to the choice when the request failed', async () => {
    await requestTag();
    await setAssignment(null, true);

    await setAssignment(null, false);

    expect(currentStep()).toBe('users.dialogs.assignRfidTag.steps.choose');
  });
});
