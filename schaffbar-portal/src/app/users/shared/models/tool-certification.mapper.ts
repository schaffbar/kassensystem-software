import { parseISO } from 'date-fns';

import { ToolCertificationApiModel } from '../../../shared/api/tool-certifications/tool-certification.api-model';
import { ToolCertification } from './tool-certification.model';

export function toToolCertification(apiModel: ToolCertificationApiModel): ToolCertification {
  return {
    id: apiModel.id,
    customerId: apiModel.customerId,
    toolId: apiModel.toolId,
    status: apiModel.status,
    certifiedAt: parseISO(apiModel.certifiedAt),
    certifiedBy: apiModel.certifiedBy,
  };
}
