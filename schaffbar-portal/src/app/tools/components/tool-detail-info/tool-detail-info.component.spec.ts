import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { ToolDetailInfoComponent, ToolDetailInfoViewModel } from './tool-detail-info.component';

describe('ToolDetailInfoComponent', () => {
  const info: ToolDetailInfoViewModel = {
    description: 'Schnittbreite 40 cm',
    area: ToolArea.FabLab,
    certificationRequirement: CertificationRequirement.Red,
    activeCertificationsCount: 24,
  };

  let fixture: ComponentFixture<ToolDetailInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolDetailInfoComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDetailInfoComponent);
    fixture.componentRef.setInput('toolDetailInfo', info);
    await fixture.whenStable();
  });

  function text(selector: string): string {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim() ?? '';
  }

  it('should show description, area, requirement and active certifications', () => {
    expect(text('.description')).toBe('Schnittbreite 40 cm');
    expect(text('.area')).toBe('tools.areas.FABLAB');
    expect(text('schbar-certification-requirement-badge .label')).toBe('tools.certificationRequirements.RED');
    expect(text('.active-certifications')).toBe('tools.pages.toolDetails.persons');
  });

  it('should explain what the requirement means', () => {
    expect(text('.requirement-hint')).toBe('info tools.certificationRequirementDescriptions.RED');
  });

  it('should say so when there is no description', async () => {
    fixture.componentRef.setInput('toolDetailInfo', { ...info, description: null });
    await fixture.whenStable();

    expect(text('.description.missing')).toBe('tools.pages.toolDetails.noDescription');
  });

  it('should request editing', () => {
    const editRequested = vi.fn();
    fixture.componentInstance.editRequested.subscribe(editRequested);

    fixture.nativeElement.querySelector('.card-header button').click();

    expect(editRequested).toHaveBeenCalledOnce();
  });
});
