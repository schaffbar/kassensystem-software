import { Pipe, PipeTransform } from '@angular/core';

import { CertificationRequirement } from '../shared/models/certification-requirement';

/**
 * Translation key of what a certification requirement means for the people using the tool.
 *
 * `{{ tool.certificationRequirement | certificationRequirementDescriptionKey | translate }}`
 */
@Pipe({ name: 'certificationRequirementDescriptionKey' })
export class CertificationRequirementDescriptionKeyPipe implements PipeTransform {
  transform(certificationRequirement: CertificationRequirement): string {
    return `tools.certificationRequirementDescriptions.${certificationRequirement}`;
  }
}
