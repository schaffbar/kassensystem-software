import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { DangerZoneComponent } from '../../../../shared/components/danger-zone/danger-zone.component';
import { AgeGroup } from '../../../../shared/models/age-group';
import { CertificationRequirement } from '../../../../shared/models/certification-requirement';
import { RfidTagAssignmentStatus } from '../../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../../shared/models/rfid-tag-assignment-type';
import { ToolArea } from '../../../../shared/models/tool-area';
import { ToolCertificationStatus } from '../../../../shared/models/tool-certification-status';
import { UserContactCardComponent } from '../../../components/user-contact-card/user-contact-card.component';
import { UserRfidTagCardComponent } from '../../../components/user-rfid-tag-card/user-rfid-tag-card.component';
import { UserToolCertificationsComponent } from '../../../components/user-tool-certifications/user-tool-certifications.component';
import { RfidTagAssignment } from '../../../rfid-tag-assignment.model';
import { ToolCertification } from '../../../tool-certification.model';
import { Tool } from '../../../tool.model';
import { User } from '../../../user.model';
import { UserDetailDialogsService } from '../user-detail-dialogs.service';
import { UserDetailStore } from '../user-detail.store';
import { UserDetailTabOverviewComponent } from './user-detail-tab-overview.component';

describe('UserDetailTabOverviewComponent', () => {
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
  const assignment: RfidTagAssignment = {
    id: 'a-1',
    customerId: 'user-1',
    rfidTagId: '04A31F9C',
    assignmentType: RfidTagAssignmentType.Fixed,
    status: RfidTagAssignmentStatus.Assigned,
    assignmentDate: new Date(2026, 1, 3),
  };
  const tool: Tool = {
    id: 'tool-1',
    name: 'Drechselbank',
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Red,
  };
  const certification: ToolCertification = {
    id: 'cert-1',
    customerId: 'user-1',
    toolId: 'tool-1',
    status: ToolCertificationStatus.Active,
    certifiedAt: new Date(2025, 2, 12),
    certifiedBy: 'instructor-1',
  };

  let fixture: ComponentFixture<UserDetailTabOverviewComponent>;
  let storeMock: {
    user: WritableSignal<User | null>;
    rfidTagAssignment: WritableSignal<RfidTagAssignment | null>;
    toolCertifications: WritableSignal<ToolCertification[]>;
    tools: WritableSignal<Tool[]>;
    userNames: WritableSignal<Map<string, string>>;
    canDeleteUser: WritableSignal<boolean>;
    unassignRfidTag: Mock;
    pauseToolCertification: Mock;
    reactivateToolCertification: Mock;
    revokeToolCertification: Mock;
    deleteToolCertification: Mock;
  };
  let dialogsMock: { editUser: Mock; assignRfidTag: Mock; deleteUser: Mock };

  beforeEach(async () => {
    storeMock = {
      user: signal(user),
      rfidTagAssignment: signal(assignment),
      toolCertifications: signal([certification]),
      tools: signal([tool]),
      userNames: signal(new Map([['instructor-1', 'Paula Richter']])),
      canDeleteUser: signal(true),
      unassignRfidTag: vi.fn(),
      pauseToolCertification: vi.fn(),
      reactivateToolCertification: vi.fn(),
      revokeToolCertification: vi.fn(),
      deleteToolCertification: vi.fn(),
    };
    dialogsMock = { editUser: vi.fn(), assignRfidTag: vi.fn(), deleteUser: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [UserDetailTabOverviewComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: UserDetailStore, useValue: storeMock },
        { provide: UserDetailDialogsService, useValue: dialogsMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailTabOverviewComponent);
    await fixture.whenStable();
  });

  function child<T>(type: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(type)).componentInstance;
  }

  function query<T extends HTMLElement = HTMLElement>(selector: string): T | null {
    return fixture.nativeElement.querySelector(selector);
  }

  it('should pass the user to the contact card and edit it in the dialog', () => {
    const card = child(UserContactCardComponent);
    expect(card.userContactCard()).toBe(user);

    card.editRequested.emit();

    expect(dialogsMock.editUser).toHaveBeenCalledTimes(1);
  });

  it('should pass the tag to the tag card, assign one in the dialog and unassign it', () => {
    const card = child(UserRfidTagCardComponent);
    expect(card.userRfidTagCard()).toEqual({
      userId: 'user-1',
      userName: 'Max Mustermann',
      assignment: {
        assignmentType: RfidTagAssignmentType.Fixed,
        status: RfidTagAssignmentStatus.Assigned,
        rfidTagId: '04A31F9C',
        assignmentDate: new Date(2026, 1, 3),
      },
    });

    card.assignmentRequested.emit();
    card.rfidTagUnassigned.emit({ userId: 'user-1' });

    expect(dialogsMock.assignRfidTag).toHaveBeenCalledTimes(1);
    expect(storeMock.unassignRfidTag).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
  });

  it('should join tool and instructor into the certification rows and execute their commands', () => {
    const certifications = child(UserToolCertificationsComponent);
    expect(certifications.variant()).toBe('card');
    expect(certifications.certifications()).toEqual([
      {
        customerId: 'user-1',
        toolId: 'tool-1',
        toolName: 'Drechselbank',
        area: ToolArea.Holz,
        certificationRequirement: CertificationRequirement.Red,
        certifierName: 'Paula Richter',
        status: ToolCertificationStatus.Active,
        certifiedAt: new Date(2025, 2, 12),
      },
    ]);

    const command = { userId: 'user-1', toolId: 'tool-1' };
    certifications.certificationPaused.emit(command);
    certifications.certificationReactivated.emit(command);
    certifications.certificationRevoked.emit(command);
    certifications.certificationDeleted.emit(command);

    expect(storeMock.pauseToolCertification).toHaveBeenCalledExactlyOnceWith(command);
    expect(storeMock.reactivateToolCertification).toHaveBeenCalledExactlyOnceWith(command);
    expect(storeMock.revokeToolCertification).toHaveBeenCalledExactlyOnceWith(command);
    expect(storeMock.deleteToolCertification).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('should show unknown tools and instructors as missing', async () => {
    storeMock.tools.set([]);
    storeMock.userNames.set(new Map());
    await fixture.whenStable();

    expect(child(UserToolCertificationsComponent).certifications()[0]).toEqual(
      expect.objectContaining({ toolName: null, area: null, certificationRequirement: null, certifierName: null }),
    );
  });

  it('should delete the user from the danger zone', () => {
    expect(child(DangerZoneComponent).message()).toBe('users.pages.userDetails.hints.dangerZone');

    query<HTMLButtonElement>('schbar-danger-zone .danger-button')?.click();

    expect(dialogsMock.deleteUser).toHaveBeenCalledTimes(1);
  });

  it('should send to the open session instead of deleting while it is open', async () => {
    storeMock.canDeleteUser.set(false);
    await fixture.whenStable();
    const openSessionRequested = vi.fn();
    fixture.componentInstance.openSessionRequested.subscribe(openSessionRequested);

    expect(child(DangerZoneComponent).message()).toBe('users.pages.userDetails.hints.dangerZoneOpenSession');
    expect(query('schbar-danger-zone .danger-button')).toBeNull();
    query<HTMLButtonElement>('schbar-danger-zone button')?.click();

    expect(openSessionRequested).toHaveBeenCalledTimes(1);
    expect(dialogsMock.deleteUser).not.toHaveBeenCalled();
  });
});
