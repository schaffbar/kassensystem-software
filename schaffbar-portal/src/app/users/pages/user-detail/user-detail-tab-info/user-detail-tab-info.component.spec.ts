import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { AgeGroup } from '../../../../shared/models/age-group';
import { ToolCertificationStatus } from '../../../../shared/models/tool-certification-status';
import { UserContactComponent } from '../../../components/user-contact/user-contact.component';
import { UserToolCertificationsComponent } from '../../../components/user-tool-certifications/user-tool-certifications.component';
import { ToolCertification } from '../../../tool-certification.model';
import { Tool } from '../../../tool.model';
import { User } from '../../../user.model';
import { UserDetailStore } from '../user-detail.store';
import { UserDetailTabInfoComponent } from './user-detail-tab-info.component';

describe('UserDetailTabInfoComponent', () => {
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

  let fixture: ComponentFixture<UserDetailTabInfoComponent>;
  let updateUserContact: Mock;

  beforeEach(async () => {
    updateUserContact = vi.fn();
    const storeMock = {
      user: signal(user),
      toolCertifications: signal(certifications),
      tools: signal(tools),
      updateUserContact,
    };

    await TestBed.configureTestingModule({
      imports: [UserDetailTabInfoComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: UserDetailStore, useValue: storeMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailTabInfoComponent);
    await fixture.whenStable();
  });

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should pass the certifications with the tool names', () => {
    expect(child(UserToolCertificationsComponent).certifications()).toEqual([
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

  it('should pass the user to the contact and execute its command', () => {
    const contact = child(UserContactComponent);
    expect(contact.userContact()).toBe(user);

    contact.contactChanged.emit({ userId: 'user-1', email: 'neu@example.org' });

    expect(updateUserContact).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1', email: 'neu@example.org' });
  });
});
