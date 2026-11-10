import { describe, expect, it } from 'vitest';

import { ToolCertificationStatus } from '../shared/models/tool-certification-status';
import {
  canPauseToolCertification,
  canReactivateToolCertification,
  canRevokeToolCertification,
} from './tool-certification.model';

describe('tool-certification.model', () => {
  it.each([
    [ToolCertificationStatus.Active, true, false, true],
    [ToolCertificationStatus.Paused, false, true, true],
    [ToolCertificationStatus.Revoked, false, false, false],
  ])('%s: pause %s, reactivate %s, revoke %s', (status, pause, reactivate, revoke) => {
    expect(canPauseToolCertification(status)).toBe(pause);
    expect(canReactivateToolCertification(status)).toBe(reactivate);
    expect(canRevokeToolCertification(status)).toBe(revoke);
  });
});
