import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { ToolCertificationStatus } from '../../../users/shared/models/tool-certification.model';
import { CertificationStatusChipComponent } from './certification-status-chip.component';

describe('CertificationStatusChipComponent', () => {
  let fixture: ComponentFixture<CertificationStatusChipComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CertificationStatusChipComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(CertificationStatusChipComponent);
  });

  it.each([
    [ToolCertificationStatus.Active, 'active'],
    [ToolCertificationStatus.Paused, 'paused'],
    [ToolCertificationStatus.Revoked, 'revoked'],
  ])('should render %s as chip with class "%s"', async (status, cssClass) => {
    fixture.componentRef.setInput('status', status);
    await fixture.whenStable();

    const chips: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('mat-chip');
    expect(chips.length).toBe(1);
    expect(chips[0].classList).toContain(cssClass);
    expect(chips[0].textContent).toContain(`users.tables.toolCertifications.statuses.${status}`);
  });
});
