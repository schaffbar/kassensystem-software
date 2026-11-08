import { describe, expect, it } from 'vitest';

import { CertificationRequirement } from '../shared/models/certification-requirement';
import { CertificationRequirementDescriptionKeyPipe } from './certification-requirement-description-key.pipe';

describe('CertificationRequirementDescriptionKeyPipe', () => {
  const pipe = new CertificationRequirementDescriptionKeyPipe();

  it('should return the translation key of the description', () => {
    expect(pipe.transform(CertificationRequirement.Yellow)).toBe('tools.certificationRequirementDescriptions.YELLOW');
    expect(pipe.transform(CertificationRequirement.Red)).toBe('tools.certificationRequirementDescriptions.RED');
  });
});
