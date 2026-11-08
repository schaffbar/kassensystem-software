import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { ToolCertificationRowViewModel, ToolCertificationsComponent } from './tool-certifications.component';

describe('ToolCertificationsComponent', () => {
  const row = (overrides: Partial<ToolCertificationRowViewModel>): ToolCertificationRowViewModel => ({
    customerId: 'user-1',
    toolId: 'tool-1',
    userName: 'Mia Fischer',
    certifierName: 'Lena Hoffmann',
    status: ToolCertificationStatus.Active,
    certifiedAt: new Date('2025-09-04T10:00:00Z'),
    ...overrides,
  });
  const certifications = [
    row({}),
    row({ customerId: 'user-2', userName: 'Anna Becker', status: ToolCertificationStatus.Paused }),
    row({
      customerId: 'user-3',
      userName: 'Ben Hartmann',
      status: ToolCertificationStatus.Revoked,
      certifierName: null,
    }),
  ];

  let fixture: ComponentFixture<ToolCertificationsComponent>;
  let openConfirmAction: Mock;
  let openConfirmDelete: Mock;

  beforeEach(async () => {
    openConfirmAction = vi.fn().mockReturnValue(of(true));
    openConfirmDelete = vi.fn().mockReturnValue(of(true));
    await TestBed.configureTestingModule({
      imports: [ToolCertificationsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: ConfirmActionDialogService, useValue: { open: openConfirmAction } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDelete } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolCertificationsComponent);
    fixture.componentRef.setInput('certifications', certifications);
    await fixture.whenStable();
  });

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  }

  async function openMenu(row: number): Promise<string[]> {
    fixture.nativeElement.querySelectorAll('tr[mat-row] schbar-row-menu button')[row].click();
    await fixture.whenStable();
    return Array.from(document.querySelectorAll<HTMLElement>('[mat-menu-item] .mat-mdc-menu-item-text')).map(
      (text) => text.textContent?.trim() ?? '',
    );
  }

  function clickMenuItem(text: string): void {
    Array.from(document.querySelectorAll<HTMLElement>('[mat-menu-item]'))
      .find((item) => item.textContent?.includes(text))
      ?.click();
  }

  async function search(text: string): Promise<void> {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = text;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('should list the certifications sorted by person, with the instructor and the state', () => {
    expect(texts('td.person')).toEqual(['Anna Becker', 'Ben Hartmann', 'Mia Fischer']);
    expect(fixture.nativeElement.querySelector('td.person a').getAttribute('href')).toBe('/users/user-2');
    expect(texts('td.mat-column-certifiedBy')).toEqual(['Lena Hoffmann', '–', 'Lena Hoffmann']);
    expect(texts('td.mat-column-status .label')).toEqual([
      'shared.certificationStatuses.PAUSED',
      'shared.certificationStatuses.REVOKED',
      'shared.certificationStatuses.ACTIVE',
    ]);
  });

  it('should search by person and say so when nobody matches', async () => {
    await search('mia');
    expect(texts('td.person')).toEqual(['Mia Fischer']);

    await search('xyz');
    expect(texts('schbar-empty-state .title')).toEqual(['tools.pages.toolDetails.certifications.noResults.title']);
  });

  it('should show the empty state without certifications', async () => {
    fixture.componentRef.setInput('certifications', []);
    await fixture.whenStable();

    expect(texts('schbar-empty-state .title')).toEqual(['tools.pages.toolDetails.certifications.empty.title']);
  });

  it('should request new certifications', () => {
    const createRequested = vi.fn();
    fixture.componentInstance.createRequested.subscribe(createRequested);

    fixture.nativeElement.querySelector('.toolbar button[matButton="filled"]').click();

    expect(createRequested).toHaveBeenCalledOnce();
  });

  describe('row menu', () => {
    it('should offer only the changes the state allows', async () => {
      expect(await openMenu(2)).toEqual([
        'tools.pages.toolDetails.certifications.actions.pause',
        'tools.pages.toolDetails.certifications.actions.revoke',
        'tools.pages.toolDetails.certifications.actions.delete',
      ]);
      document.querySelector('.cdk-overlay-container')?.replaceChildren();

      expect(await openMenu(0)).toEqual([
        'tools.pages.toolDetails.certifications.actions.reactivate',
        'tools.pages.toolDetails.certifications.actions.revoke',
        'tools.pages.toolDetails.certifications.actions.delete',
      ]);
      document.querySelector('.cdk-overlay-container')?.replaceChildren();

      expect(await openMenu(1)).toEqual(['tools.pages.toolDetails.certifications.actions.delete']);
    });

    it('should pause a certification after confirmation', async () => {
      const paused = vi.fn();
      fixture.componentInstance.certificationPaused.subscribe(paused);
      await openMenu(2);

      clickMenuItem('actions.pause');

      expect(openConfirmAction).toHaveBeenCalledExactlyOnceWith({
        title: 'tools.dialogs.pauseCertification.title',
        message: 'tools.dialogs.pauseCertification.message',
        confirmLabel: 'tools.dialogs.pauseCertification.confirm',
        entity: 'Mia Fischer',
        destructive: true,
      });
      expect(paused).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1', toolId: 'tool-1' });
    });

    it('should reactivate a certification after confirmation, without the danger color', async () => {
      const reactivated = vi.fn();
      fixture.componentInstance.certificationReactivated.subscribe(reactivated);
      await openMenu(0);

      clickMenuItem('actions.reactivate');

      expect(openConfirmAction).toHaveBeenCalledWith(expect.objectContaining({ destructive: false }));
      expect(reactivated).toHaveBeenCalledExactlyOnceWith({ userId: 'user-2', toolId: 'tool-1' });
    });

    it('should revoke a certification after confirmation', async () => {
      const revoked = vi.fn();
      fixture.componentInstance.certificationRevoked.subscribe(revoked);
      await openMenu(0);

      clickMenuItem('actions.revoke');

      expect(openConfirmAction).toHaveBeenCalledWith(
        expect.objectContaining({ title: 'tools.dialogs.revokeCertification.title', destructive: true }),
      );
      expect(revoked).toHaveBeenCalledExactlyOnceWith({ userId: 'user-2', toolId: 'tool-1' });
    });

    it('should delete a certification after confirmation', async () => {
      const deleted = vi.fn();
      fixture.componentInstance.certificationDeleted.subscribe(deleted);
      await openMenu(1);

      clickMenuItem('actions.delete');

      expect(openConfirmDelete).toHaveBeenCalledWith(expect.objectContaining({ entity: 'Ben Hartmann' }));
      expect(deleted).toHaveBeenCalledExactlyOnceWith({ userId: 'user-3', toolId: 'tool-1' });
    });

    it('should change nothing when the confirmation is cancelled', async () => {
      openConfirmAction.mockReturnValue(of(false));
      const paused = vi.fn();
      fixture.componentInstance.certificationPaused.subscribe(paused);
      await openMenu(2);

      clickMenuItem('actions.pause');

      expect(paused).not.toHaveBeenCalled();
    });
  });
});
