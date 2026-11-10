import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { CertificationRequirement } from '../../models/certification-requirement';
import { CertificationRequirementBadgeComponent } from './certification-requirement-badge.component';

describe('CertificationRequirementBadgeComponent', () => {
  let fixture: ComponentFixture<CertificationRequirementBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CertificationRequirementBadgeComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(CertificationRequirementBadgeComponent);
  });

  it.each([
    [CertificationRequirement.Yellow, 'tone-warn', 'info'],
    [CertificationRequirement.Red, 'tone-danger', 'shield'],
  ])('should render %s as badge with %s and icon %s', async (certificationRequirement, toneClass, icon) => {
    fixture.componentRef.setInput('certificationRequirement', certificationRequirement);
    await fixture.whenStable();

    const badge: HTMLElement = fixture.nativeElement.querySelector('schbar-status-badge');
    expect(badge.classList).toContain(toneClass);
    expect(badge.querySelector('mat-icon')?.textContent).toBe(icon);
    expect(badge.textContent).toContain(`shared.certificationRequirements.${certificationRequirement}`);
  });
});
