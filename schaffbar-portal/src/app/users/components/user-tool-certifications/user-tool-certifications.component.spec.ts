import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import {
  UserToolCertificationRowViewModel,
  UserToolCertificationsComponent,
  UserToolCertificationsVariant,
} from './user-tool-certifications.component';

describe('UserToolCertificationsComponent', () => {
  function certification(
    toolName: string | null,
    status: ToolCertificationStatus,
    overrides: Partial<UserToolCertificationRowViewModel> = {},
  ): UserToolCertificationRowViewModel {
    return {
      customerId: 'user-1',
      toolId: `tool-${toolName}`,
      toolName,
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
      certifierName: 'Paula Richter',
      status,
      certifiedAt: new Date(2025, 2, 12),
      ...overrides,
    };
  }

  let fixture: ComponentFixture<UserToolCertificationsComponent>;
  let openConfirmAction: Mock;
  let openConfirmDelete: Mock;

  beforeEach(() => {
    openConfirmAction = vi.fn().mockReturnValue(of(true));
    openConfirmDelete = vi.fn().mockReturnValue(of(true));
    TestBed.configureTestingModule({
      imports: [UserToolCertificationsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ConfirmActionDialogService, useValue: { open: openConfirmAction } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDelete } },
      ],
    });
    fixture = TestBed.createComponent(UserToolCertificationsComponent);
  });

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  async function render(
    certifications: UserToolCertificationRowViewModel[],
    variant: UserToolCertificationsVariant = 'table',
  ): Promise<void> {
    fixture.componentRef.setInput('certifications', certifications);
    fixture.componentRef.setInput('variant', variant);
    await fixture.whenStable();
  }

  function texts(selector: string): string[] {
    return [...fixture.nativeElement.querySelectorAll(selector)].map((element: HTMLElement) =>
      element.textContent!.replace(/\s+/g, ' ').trim(),
    );
  }

  async function openMenu(): Promise<HTMLElement[]> {
    fixture.nativeElement.querySelector('schbar-row-menu button').click();
    await fixture.whenStable();
    return [...document.querySelectorAll<HTMLElement>('.mat-mdc-menu-item')];
  }

  describe('card', () => {
    it('should list the certifications sorted by tool, with area, date and instructor', async () => {
      await render(
        [
          certification('Lasercutter', ToolCertificationStatus.Paused, { certifierName: null }),
          certification('Bandsäge', ToolCertificationStatus.Active),
        ],
        'card',
      );

      expect(texts('.tool-name')).toEqual(['Bandsäge', 'Lasercutter']);
      expect(texts('.certification-detail')).toEqual([
        'shared.toolAreas.HOLZ · users.pages.userDetails.certifications.certifiedOnBy',
        'shared.toolAreas.HOLZ · users.pages.userDetails.certifications.certifiedOn',
      ]);
      expect(fixture.nativeElement.querySelector('table')).toBeNull();
    });

    it('should count only the statuses that occur', async () => {
      await render(
        [
          certification('Bandsäge', ToolCertificationStatus.Active),
          certification('Drechselbank', ToolCertificationStatus.Active),
          certification('Schweißgerät', ToolCertificationStatus.Revoked),
        ],
        'card',
      );

      expect(texts('.status-counts')).toEqual([
        'users.pages.userDetails.certifications.counts.ACTIVE · users.pages.userDetails.certifications.counts.REVOKED',
      ]);
    });

    it('should show an empty state', async () => {
      await render([], 'card');

      expect(fixture.nativeElement.querySelector('schbar-empty-state')).not.toBeNull();
      expect(texts('.status-counts')).toEqual([]);
    });
  });

  describe('table', () => {
    it('should show tool, area, requirement, date, instructor and status', async () => {
      await render([certification('Bandsäge', ToolCertificationStatus.Active)]);

      const cells = texts('tr.mat-mdc-row td');
      expect(cells.slice(0, 5)).toEqual([
        'Bandsägeshared.toolAreas.HOLZ',
        'shield shared.certificationRequirements.RED',
        expect.any(String),
        'Paula Richter',
        'check_circle shared.certificationStatuses.ACTIVE',
      ]);
    });

    it('should show a deleted tool as dash', async () => {
      await render([
        certification(null, ToolCertificationStatus.Active, {
          area: null,
          certificationRequirement: null,
          certifierName: null,
        }),
      ]);

      const cells = texts('tr.mat-mdc-row td');
      expect(cells[0]).toBe('–');
      expect(cells[1]).toBe('–');
      expect(cells[3]).toBe('–');
    });
  });

  describe('actions', () => {
    it('should offer pause, revoke and delete for an active certification', async () => {
      await render([certification('Bandsäge', ToolCertificationStatus.Active)]);

      const items = await openMenu();

      expect(items.map((item) => item.textContent!.trim())).toEqual([
        'pause_circleusers.pages.userDetails.certifications.actions.pause',
        'blockusers.pages.userDetails.certifications.actions.revoke',
        'deleteusers.pages.userDetails.certifications.actions.delete',
      ]);
    });

    it('should offer only delete for a revoked certification', async () => {
      await render([certification('Bandsäge', ToolCertificationStatus.Revoked)]);

      const items = await openMenu();

      expect(items.map((item) => item.textContent!.trim())).toEqual([
        'deleteusers.pages.userDetails.certifications.actions.delete',
      ]);
    });

    it('should pause after confirmation, naming the tool', async () => {
      await render([certification('Bandsäge', ToolCertificationStatus.Active)]);
      const certificationPaused = vi.fn();
      fixture.componentInstance.certificationPaused.subscribe(certificationPaused);

      (await openMenu())[0].click();

      expect(openConfirmAction).toHaveBeenCalledWith(
        expect.objectContaining({
          entity: 'Bandsäge',
          confirmLabel: 'users.dialogs.certifications.pauseCertification.confirm',
          destructive: true,
        }),
      );
      expect(certificationPaused).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1', toolId: 'tool-Bandsäge' });
    });

    it('should reactivate a paused certification without danger', async () => {
      await render([certification('Bandsäge', ToolCertificationStatus.Paused)]);
      const certificationReactivated = vi.fn();
      fixture.componentInstance.certificationReactivated.subscribe(certificationReactivated);

      (await openMenu())[0].click();

      expect(openConfirmAction).toHaveBeenCalledWith(expect.objectContaining({ destructive: false }));
      expect(certificationReactivated).toHaveBeenCalledTimes(1);
    });

    it('should revoke and delete after confirmation', async () => {
      await render([certification('Bandsäge', ToolCertificationStatus.Active)]);
      const certificationRevoked = vi.fn();
      const certificationDeleted = vi.fn();
      fixture.componentInstance.certificationRevoked.subscribe(certificationRevoked);
      fixture.componentInstance.certificationDeleted.subscribe(certificationDeleted);

      (await openMenu())[1].click();
      (await openMenu())[2].click();

      expect(certificationRevoked).toHaveBeenCalledTimes(1);
      expect(openConfirmDelete).toHaveBeenCalledWith(
        expect.objectContaining({ confirmLabel: 'users.dialogs.certifications.deleteCertification.confirm' }),
      );
      expect(certificationDeleted).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1', toolId: 'tool-Bandsäge' });
    });

    it('should do nothing without confirmation', async () => {
      openConfirmAction.mockReturnValue(of(false));
      await render([certification('Bandsäge', ToolCertificationStatus.Active)]);
      const certificationPaused = vi.fn();
      fixture.componentInstance.certificationPaused.subscribe(certificationPaused);

      (await openMenu())[0].click();

      expect(certificationPaused).not.toHaveBeenCalled();
    });
  });
});
