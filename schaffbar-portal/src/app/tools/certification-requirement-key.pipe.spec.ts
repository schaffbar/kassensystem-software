import { describe, expect, it } from 'vitest';

import { CertificationRequirement } from '../shared/models/certification-requirement';
import { CertificationRequirementKeyPipe } from './certification-requirement-key.pipe';

describe('CertificationRequirementKeyPipe', () => {
  it('should return the translation key of the certification requirement', () => {
    expect(new CertificationRequirementKeyPipe().transform(CertificationRequirement.Red)).toBe(
      'tools.certificationRequirements.RED',
    );
  });
});
