import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { CertificationRequirementDotComponent } from './certification-requirement-dot.component';

describe('CertificationRequirementDotComponent', () => {
  let fixture: ComponentFixture<CertificationRequirementDotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CertificationRequirementDotComponent],
      providers: [provideZonelessChangeDetection()],
    }).compileComponents();

    fixture = TestBed.createComponent(CertificationRequirementDotComponent);
  });

  it.each([
    [CertificationRequirement.Yellow, 'dot--yellow'],
    [CertificationRequirement.Red, 'dot--red'],
  ])('should show the colour of %s', async (certificationRequirement, colourClass) => {
    fixture.componentRef.setInput('certificationRequirement', certificationRequirement);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.dot').classList).toContain(colourClass);
  });
});
