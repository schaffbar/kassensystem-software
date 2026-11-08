import { describe, expect, it } from 'vitest';

import { ToolCertificationStatus } from '../shared/models/tool-certification-status';
import {
  canPauseToolCertification,
  canReactivateToolCertification,
  canRevokeToolCertification,
} from './tool-certification.model';

describe('tool certification status changes', () => {
  const { Active, Paused, Revoked } = ToolCertificationStatus;

  it('should pause only an active certification', () => {
    expect([Active, Paused, Revoked].map(canPauseToolCertification)).toEqual([true, false, false]);
  });

  it('should reactivate only a paused certification', () => {
    expect([Active, Paused, Revoked].map(canReactivateToolCertification)).toEqual([false, true, false]);
  });

  it('should revoke an active or paused certification, but not a revoked one', () => {
    expect([Active, Paused, Revoked].map(canRevokeToolCertification)).toEqual([true, true, false]);
  });
});
