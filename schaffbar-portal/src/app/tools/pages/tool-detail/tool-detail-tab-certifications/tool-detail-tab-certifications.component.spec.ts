import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ToolCertificationStatus } from '../../../../shared/models/tool-certification-status';
import { ToolCertificationsComponent } from '../../../components/tool-certifications/tool-certifications.component';
import { ToolCertification } from '../../../tool-certification.model';
import { User } from '../../../user.model';
import { ToolDetailStore } from '../tool-detail.store';
import { ToolDetailTabCertificationsComponent } from './tool-detail-tab-certifications.component';

describe('ToolDetailTabCertificationsComponent', () => {
  const users: User[] = [{ id: 'user-1', firstName: 'Max', lastName: 'Mustermann' }];
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
      customerId: 'user-deleted',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Paused,
      certifiedAt: new Date('2026-02-01T10:00:00Z'),
      certifiedBy: null,
    },
  ];

  let fixture: ComponentFixture<ToolDetailTabCertificationsComponent>;
  let deleteCertification: Mock;

  beforeEach(async () => {
    deleteCertification = vi.fn();
    const detailsStoreMock = {
      tool: signal({ id: 'tool-1' }),
      toolCertifications: signal(certifications),
      users: signal(users),
      certifiableUsers: signal([]),
      certifiers: signal([]),
      deleteCertification,
    };

    await TestBed.configureTestingModule({
      imports: [ToolDetailTabCertificationsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ToolDetailStore, useValue: detailsStoreMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDetailTabCertificationsComponent);
    await fixture.whenStable();
  });

  function certificationsComponent(): ToolCertificationsComponent {
    return fixture.debugElement.query(By.directive(ToolCertificationsComponent)).componentInstance;
  }

  it('should pass the certifications with the user names', () => {
    expect(certificationsComponent().certifications()).toEqual([
      {
        customerId: 'user-1',
        toolId: 'tool-1',
        userName: 'Max Mustermann',
        status: ToolCertificationStatus.Active,
        certifiedAt: new Date('2026-01-01T10:00:00Z'),
      },
      {
        customerId: 'user-deleted',
        toolId: 'tool-1',
        userName: null,
        status: ToolCertificationStatus.Paused,
        certifiedAt: new Date('2026-02-01T10:00:00Z'),
      },
    ]);
  });

  it('should execute the delete command', () => {
    certificationsComponent().certificationDeleted.emit({ userId: 'user-1', toolId: 'tool-1' });

    expect(deleteCertification).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1', toolId: 'tool-1' });
  });
});
