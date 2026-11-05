import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { AgeGroup } from '../../../shared/models/age-group';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { UserDetailHeaderComponent } from '../../components/user-detail-header/user-detail-header.component';
import { UserToolCertificationsComponent } from '../../components/user-tool-certifications/user-tool-certifications.component';
import { ToolCertification } from '../../tool-certification.model';
import { Tool } from '../../tool.model';
import { User } from '../../user.model';
import { UserDetailComponent } from './user-detail.component';
import { UserDetailStore } from './user-detail.store';

describe('UserDetailComponent', () => {
  const user: User = {
    id: 'user-1',
    firstName: 'Max',
    lastName: 'Mustermann',
    dateOfBirth: new Date(1990, 0, 31),
    ageGroup: AgeGroup.Adult,
    clubMember: false,
    email: 'max@example.org',
    phone: null,
    address: {
      addressLine1: 'Hauptstraße 1',
      addressLine2: null,
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    },
    createdAt: new Date('2026-01-01T10:00:00Z'),
    updatedAt: new Date('2026-01-01T10:00:00Z'),
  };
  const tools: Tool[] = [{ id: 'tool-1', name: 'Bandsäge' }];
  const certifications: ToolCertification[] = [
    {
      id: 'cert-1',
      customerId: 'user-1',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Active,
      certifiedAt: new Date('2026-01-01T10:00:00Z'),
      certifiedBy: null,
    },
    {
      id: 'cert-2',
      customerId: 'user-1',
      toolId: 'tool-deleted',
      status: ToolCertificationStatus.Revoked,
      certifiedAt: new Date('2026-02-01T10:00:00Z'),
      certifiedBy: null,
    },
  ];

  let fixture: ComponentFixture<UserDetailComponent>;
  let assignRfidTag: Mock;

  beforeEach(async () => {
    assignRfidTag = vi.fn();
    const storeMock = {
      isLoading: signal(false),
      hasLoadError: signal(false),
      user: signal(user),
      rfidTagAssignment: signal(null),
      toolCertifications: signal(certifications),
      tools: signal(tools),
      // read by the open session tab
      openSession: signal(null),
      setUserId: vi.fn(),
      assignRfidTag,
    };

    await TestBed.configureTestingModule({
      imports: [UserDetailComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    })
      .overrideComponent(UserDetailComponent, {
        set: { providers: [{ provide: UserDetailStore, useValue: storeMock }] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(UserDetailComponent);
    fixture.componentRef.setInput('id', 'user-1');
    await fixture.whenStable();
  });

  it('should pass the certifications with the tool names', () => {
    const certificationsComponent: UserToolCertificationsComponent = fixture.debugElement.query(
      By.directive(UserToolCertificationsComponent),
    ).componentInstance;

    expect(certificationsComponent.certifications()).toEqual([
      {
        customerId: 'user-1',
        toolId: 'tool-1',
        toolName: 'Bandsäge',
        status: ToolCertificationStatus.Active,
        certifiedAt: new Date('2026-01-01T10:00:00Z'),
      },
      {
        customerId: 'user-1',
        toolId: 'tool-deleted',
        toolName: null,
        status: ToolCertificationStatus.Revoked,
        certifiedAt: new Date('2026-02-01T10:00:00Z'),
      },
    ]);
  });

  it('should pass the user to the header and execute its commands', () => {
    const header: UserDetailHeaderComponent = fixture.debugElement.query(
      By.directive(UserDetailHeaderComponent),
    ).componentInstance;
    expect(header.userDetailHeader()).toBe(user);
    expect(header.rfidTagAssignment()).toBeNull();

    header.assignRfidTag.emit({ customerId: 'user-1', assignmentType: RfidTagAssignmentType.Fixed });

    expect(assignRfidTag).toHaveBeenCalledExactlyOnceWith({
      customerId: 'user-1',
      assignmentType: RfidTagAssignmentType.Fixed,
    });
  });
});
