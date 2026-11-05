import { describe, expect, it } from 'vitest';

import { ToolCertificationStatus } from '../shared/models/tool-certification-status';
import { toToolCertification } from './tool-certification.mapper';

describe('toToolCertification', () => {
  it('should map the API model and parse the certification date', () => {
    expect(
      toToolCertification({
        id: 'cert-1',
        customerId: 'user-1',
        toolId: 'tool-1',
        status: ToolCertificationStatus.Paused,
        certifiedAt: '2026-01-01T10:00:00Z',
        certifiedBy: null,
      }),
    ).toEqual({
      id: 'cert-1',
      customerId: 'user-1',
      toolId: 'tool-1',
      status: ToolCertificationStatus.Paused,
      certifiedAt: new Date('2026-01-01T10:00:00Z'),
      certifiedBy: null,
    });
  });
});
