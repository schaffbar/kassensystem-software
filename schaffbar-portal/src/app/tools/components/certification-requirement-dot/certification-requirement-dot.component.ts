import { LowerCasePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';

@Component({
  selector: 'schbar-certification-requirement-dot',
  templateUrl: './certification-requirement-dot.component.html',
  styleUrl: './certification-requirement-dot.component.scss',
  imports: [LowerCasePipe],
})
export class CertificationRequirementDotComponent {
  readonly certificationRequirement = input.required<CertificationRequirement>();
}
