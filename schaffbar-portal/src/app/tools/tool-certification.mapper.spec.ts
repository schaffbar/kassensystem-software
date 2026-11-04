import { describe, expect, it } from 'vitest';

import { ToolCertificationStatus } from '../shared/models/tool-certification-status';
import { toBatchCertifyRequest, toToolCertification } from './tool-certification.mapper';

describe('tool-certification.mapper', () => {
  describe('toToolCertification', () => {
    it('should map the API model and parse the certification date', () => {
      expect(
        toToolCertification({
          id: 'cert-1',
          customerId: 'user-1',
          toolId: 'tool-1',
          status: ToolCertificationStatus.Active,
          certifiedAt: '2026-01-01T10:00:00Z',
          certifiedBy: 'instructor-1',
        }),
      ).toEqual({
        id: 'cert-1',
        customerId: 'user-1',
        toolId: 'tool-1',
        status: ToolCertificationStatus.Active,
        certifiedAt: new Date('2026-01-01T10:00:00Z'),
        certifiedBy: 'instructor-1',
      });
    });
  });

  describe('toBatchCertifyRequest', () => {
    it('should send customers, tool and instructor', () => {
      expect(
        toBatchCertifyRequest({ customerIds: ['user-1', 'user-2'], toolId: 'tool-1', certifiedBy: 'instructor-1' }),
      ).toEqual({ customerIds: ['user-1', 'user-2'], toolId: 'tool-1', certifiedBy: 'instructor-1' });
    });
  });
});
