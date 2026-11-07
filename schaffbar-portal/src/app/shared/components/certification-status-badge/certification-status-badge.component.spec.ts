import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { ToolCertificationStatus } from '../../models/tool-certification-status';
import { CertificationStatusBadgeComponent } from './certification-status-badge.component';

describe('CertificationStatusBadgeComponent', () => {
  let fixture: ComponentFixture<CertificationStatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CertificationStatusBadgeComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(CertificationStatusBadgeComponent);
  });

  it.each([
    [ToolCertificationStatus.Active, 'tone-success', 'check_circle'],
    [ToolCertificationStatus.Paused, 'tone-warn', 'pause_circle'],
    [ToolCertificationStatus.Revoked, 'tone-danger', 'block'],
  ])('should render %s as badge with %s and icon %s', async (status, toneClass, icon) => {
    fixture.componentRef.setInput('status', status);
    await fixture.whenStable();

    const badge: HTMLElement = fixture.nativeElement.querySelector('schbar-status-badge');
    expect(badge.classList).toContain(toneClass);
    expect(badge.querySelector('mat-icon')?.textContent).toBe(icon);
    expect(badge.textContent).toContain(`shared.certificationStatuses.${status}`);
  });
});
