import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ToolCertificationStatus } from '../../../../shared/models/tool-certification-status';
import { CreateToolCertificationsFormComponent } from '../../../components/create-tool-certifications-form/create-tool-certifications-form.component';
import { ToolCertificationsComponent } from '../../../components/tool-certifications/tool-certifications.component';
import { BatchCreateToolCertificationsCommand, ToolCertification } from '../../../tool-certification.model';
import { User } from '../../../user.model';
import { ToolDetailStore } from '../tool-detail.store';
import { ToolDetailTabCertificationsComponent } from './tool-detail-tab-certifications.component';

describe('ToolDetailTabCertificationsComponent', () => {
  const users: User[] = [
    { id: 'user-1', firstName: 'Lena', lastName: 'Hoffmann', email: 'lena@example.org' },
    { id: 'user-2', firstName: 'Mia', lastName: 'Fischer', email: 'mia@example.org' },
  ];
  const certifications: ToolCertification[] = [
    {
      id: 'cert-1',
      customerId: 'user-2',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Active,
      certifiedAt: new Date('2025-09-04T10:00:00Z'),
      certifiedBy: 'user-1',
    },
    {
      id: 'cert-2',
      customerId: 'deleted-user',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Paused,
      certifiedAt: new Date('2025-01-01T10:00:00Z'),
      certifiedBy: null,
    },
  ];

  let fixture: ComponentFixture<ToolDetailTabCertificationsComponent>;
  let storeMock: Record<string, unknown> &
    Record<
      | 'batchCreateToolCertifications'
      | 'pauseToolCertification'
      | 'reactivateToolCertification'
      | 'revokeToolCertification'
      | 'deleteToolCertification',
      Mock
    >;
  let openDialog: Mock;

  beforeEach(async () => {
    openDialog = vi.fn();
    storeMock = {
      tool: signal({ id: 'tool-1', name: 'Lasercutter' }),
      toolCertifications: signal(certifications),
      users: signal(users),
      certifiers: signal([users[0]]),
      certifiableUsers: signal([users[1]]),
      batchCreateToolCertifications: vi.fn(),
      pauseToolCertification: vi.fn(),
      reactivateToolCertification: vi.fn(),
      revokeToolCertification: vi.fn(),
      deleteToolCertification: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ToolDetailTabCertificationsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: ToolDetailStore, useValue: storeMock },
        { provide: MatDialog, useValue: { open: openDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDetailTabCertificationsComponent);
    await fixture.whenStable();
  });

  function certificationsComponent(): ToolCertificationsComponent {
    return fixture.debugElement.query(By.directive(ToolCertificationsComponent)).componentInstance;
  }

  it('should name the person and the instructor of each certification', () => {
    expect(certificationsComponent().certifications()).toEqual([
      {
        customerId: 'user-2',
        toolId: 'tool-1',
        userName: 'Mia Fischer',
        certifierName: 'Lena Hoffmann',
        status: ToolCertificationStatus.Active,
        certifiedAt: new Date('2025-09-04T10:00:00Z'),
      },
      expect.objectContaining({ userName: null, certifierName: null }),
    ]);
  });

  it('should open the form with the instructors and the people without certification', () => {
    const command: BatchCreateToolCertificationsCommand = {
      userIds: ['user-2'],
      toolId: 'tool-1',
      certifiedBy: 'user-1',
    };
    openDialog.mockReturnValue({ afterClosed: () => of(command) });

    certificationsComponent().createRequested.emit();

    expect(openDialog).toHaveBeenCalledWith(
      CreateToolCertificationsFormComponent,
      expect.objectContaining({
        data: { toolId: 'tool-1', toolName: 'Lasercutter', certifiers: [users[0]], candidates: [users[1]] },
      }),
    );
    expect(storeMock.batchCreateToolCertifications).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('should execute the status changes of the table', () => {
    const command = { userId: 'user-2', toolId: 'tool-1' };
    const table = certificationsComponent();

    table.certificationPaused.emit(command);
    table.certificationReactivated.emit(command);
    table.certificationRevoked.emit(command);
    table.certificationDeleted.emit(command);

    expect(storeMock.pauseToolCertification).toHaveBeenCalledExactlyOnceWith(command);
    expect(storeMock.reactivateToolCertification).toHaveBeenCalledExactlyOnceWith(command);
    expect(storeMock.revokeToolCertification).toHaveBeenCalledExactlyOnceWith(command);
    expect(storeMock.deleteToolCertification).toHaveBeenCalledExactlyOnceWith(command);
  });
});
