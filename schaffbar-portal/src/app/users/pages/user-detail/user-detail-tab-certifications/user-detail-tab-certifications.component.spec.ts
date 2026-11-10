import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement } from '../../../../shared/models/certification-requirement';
import { ToolArea } from '../../../../shared/models/tool-area';
import { ToolCertificationStatus } from '../../../../shared/models/tool-certification-status';
import { UserToolCertificationsComponent } from '../../../components/user-tool-certifications/user-tool-certifications.component';
import { UserDetailStore } from '../user-detail.store';
import { UserDetailTabCertificationsComponent } from './user-detail-tab-certifications.component';

describe('UserDetailTabCertificationsComponent', () => {
  let fixture: ComponentFixture<UserDetailTabCertificationsComponent>;
  let pauseToolCertification: Mock;
  let deleteToolCertification: Mock;

  beforeEach(async () => {
    pauseToolCertification = vi.fn();
    deleteToolCertification = vi.fn();
    const storeMock = {
      toolCertifications: signal([
        {
          id: 'cert-1',
          customerId: 'user-1',
          toolId: 'tool-1',
          status: ToolCertificationStatus.Paused,
          certifiedAt: new Date(2025, 2, 12),
          certifiedBy: null,
        },
      ]),
      tools: signal([
        {
          id: 'tool-1',
          name: 'Lasercutter',
          area: ToolArea.FabLab,
          certificationRequirement: CertificationRequirement.Red,
        },
      ]),
      userNames: signal(new Map()),
      pauseToolCertification,
      reactivateToolCertification: vi.fn(),
      revokeToolCertification: vi.fn(),
      deleteToolCertification,
    };

    await TestBed.configureTestingModule({
      imports: [UserDetailTabCertificationsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: UserDetailStore, useValue: storeMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailTabCertificationsComponent);
    await fixture.whenStable();
  });

  it('should show all certifications as table and execute their commands', () => {
    const certifications: UserToolCertificationsComponent = fixture.debugElement.query(
      By.directive(UserToolCertificationsComponent),
    ).componentInstance;
    expect(certifications.variant()).toBe('table');
    expect(certifications.certifications()).toEqual([
      expect.objectContaining({ toolName: 'Lasercutter', area: ToolArea.FabLab, certifierName: null }),
    ]);

    const command = { userId: 'user-1', toolId: 'tool-1' };
    certifications.certificationPaused.emit(command);
    certifications.certificationDeleted.emit(command);

    expect(pauseToolCertification).toHaveBeenCalledExactlyOnceWith(command);
    expect(deleteToolCertification).toHaveBeenCalledExactlyOnceWith(command);
  });
});
