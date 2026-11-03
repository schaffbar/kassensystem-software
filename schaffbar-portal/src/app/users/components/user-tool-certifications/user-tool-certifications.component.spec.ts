import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { CertificationRequirement, Tool, ToolArea } from '../../../tools/tool.model';
import { ToolCertification, ToolCertificationStatus } from '../../../tool-certifications/tool-certification.model';
import { UserToolCertificationsComponent } from './user-tool-certifications.component';

describe('UserToolCertificationsComponent', () => {
  const tool: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
    description: '',
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Yellow,
    rfidReaderId: null,
    wlanRelaisType: null,
    ipAddress: null,
    httpStartCommand: null,
    onCommand: null,
    offCommand: null,
    instructors: [],
  };

  function certification(status: ToolCertificationStatus): ToolCertification {
    return {
      id: 'cert-1',
      customerId: 'user-1',
      toolId: 'tool-1',
      status,
      certifiedAt: '2026-01-01T10:00:00Z',
      certifiedBy: null,
    };
  }

  let fixture: ComponentFixture<UserToolCertificationsComponent>;
  let openConfirmActionDialog: Mock;

  beforeEach(() => {
    openConfirmActionDialog = vi.fn().mockReturnValue(of(true));
    TestBed.configureTestingModule({
      imports: [UserToolCertificationsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ConfirmActionDialogService, useValue: { open: openConfirmActionDialog } },
      ],
    });
    fixture = TestBed.createComponent(UserToolCertificationsComponent);
    fixture.componentRef.setInput('allTools', [tool]);
  });

  async function render(status: ToolCertificationStatus): Promise<void> {
    fixture.componentRef.setInput('certifications', [certification(status)]);
    await fixture.whenStable();
  }

  function clickAction(title: string): void {
    fixture.nativeElement.querySelector(`button[title="users.actions.toolCertifications.${title}"]`).click();
  }

  it('should pause after a destructive confirmation', async () => {
    const paused = vi.fn();
    fixture.componentInstance.pauseCertification.subscribe(paused);
    await render(ToolCertificationStatus.Active);

    clickAction('pause');

    expect(openConfirmActionDialog).toHaveBeenCalledExactlyOnceWith({
      title: 'users.dialogs.certifications.pauseCertification.title',
      message: 'users.dialogs.certifications.pauseCertification.message',
      entity: 'Bandsäge',
      confirmLabel: 'users.dialogs.certifications.pauseCertification.confirm',
      destructive: true,
    });
    expect(paused).toHaveBeenCalledExactlyOnceWith({ customerId: 'user-1', toolId: 'tool-1' });
  });

  it('should reactivate after a non-destructive confirmation', async () => {
    const reactivated = vi.fn();
    fixture.componentInstance.reactivateCertification.subscribe(reactivated);
    await render(ToolCertificationStatus.Paused);

    clickAction('reactivate');

    expect(openConfirmActionDialog).toHaveBeenCalledExactlyOnceWith(
      expect.objectContaining({
        confirmLabel: 'users.dialogs.certifications.reactivateCertification.confirm',
        destructive: false,
      }),
    );
    expect(reactivated).toHaveBeenCalledExactlyOnceWith({ customerId: 'user-1', toolId: 'tool-1' });
  });

  it('should revoke after a destructive confirmation', async () => {
    const revoked = vi.fn();
    fixture.componentInstance.revokeCertification.subscribe(revoked);
    await render(ToolCertificationStatus.Active);

    clickAction('revoke');

    expect(openConfirmActionDialog).toHaveBeenCalledExactlyOnceWith(
      expect.objectContaining({
        confirmLabel: 'users.dialogs.certifications.revokeCertification.confirm',
        destructive: true,
      }),
    );
    expect(revoked).toHaveBeenCalledExactlyOnceWith({ customerId: 'user-1', toolId: 'tool-1' });
  });

  it('should emit nothing when cancelled', async () => {
    openConfirmActionDialog.mockReturnValue(of(false));
    const paused = vi.fn();
    fixture.componentInstance.pauseCertification.subscribe(paused);
    await render(ToolCertificationStatus.Active);

    clickAction('pause');

    expect(paused).not.toHaveBeenCalled();
  });
});
