import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import {
  UserToolCertificationRowViewModel,
  UserToolCertificationsComponent,
} from './user-tool-certifications.component';

describe('UserToolCertificationsComponent', () => {
  function certification(status: ToolCertificationStatus): UserToolCertificationRowViewModel {
    return {
      customerId: 'user-1',
      toolId: 'tool-1',
      toolName: 'Bandsäge',
      status,
      certifiedAt: new Date('2026-01-01T10:00:00Z'),
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
  });

  async function render(status: ToolCertificationStatus): Promise<void> {
    fixture.componentRef.setInput('certifications', [certification(status)]);
    await fixture.whenStable();
  }

  function clickAction(title: string): void {
    fixture.nativeElement.querySelector(`button[title="users.actions.toolCertifications.${title}"]`).click();
  }

  it('should show no actions for a revoked certification', async () => {
    await render(ToolCertificationStatus.Revoked);

    const actions: HTMLElement = fixture.nativeElement.querySelector('td.mat-column-actions');
    expect(actions.querySelectorAll('button').length).toBe(0);
    expect(actions.textContent?.trim()).toBe('');
  });

  it('should pause after a destructive confirmation', async () => {
    const paused = vi.fn();
    fixture.componentInstance.certificationPaused.subscribe(paused);
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
    fixture.componentInstance.certificationReactivated.subscribe(reactivated);
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
    fixture.componentInstance.certificationRevoked.subscribe(revoked);
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
    fixture.componentInstance.certificationPaused.subscribe(paused);
    await render(ToolCertificationStatus.Active);

    clickAction('pause');

    expect(paused).not.toHaveBeenCalled();
  });
});
