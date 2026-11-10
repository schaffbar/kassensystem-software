import { Pipe, PipeTransform } from '@angular/core';

import { CertificationRequirement } from '../models/certification-requirement';

/**
 * Translation key of a certification requirement.
 *
 * `{{ tool.certificationRequirement | certificationRequirementKey | translate }}`
 */
@Pipe({ name: 'certificationRequirementKey' })
export class CertificationRequirementKeyPipe implements PipeTransform {
  transform(certificationRequirement: CertificationRequirement): string {
    return `shared.certificationRequirements.${certificationRequirement}`;
  }
}
