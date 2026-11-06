import { parseISO } from 'date-fns';

import {
  BatchCertifyRequest,
  ToolCertificationApiModel,
} from '../shared/api/tool-certifications/tool-certification.api-model';
import { BatchCreateToolCertificationCommand, ToolCertification } from './tool-certification.model';

// ----------------------------------------------------------------------------
// API model → feature model

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

// ----------------------------------------------------------------------------
// command → request

export function toBatchCertifyRequest(command: BatchCreateToolCertificationCommand): BatchCertifyRequest {
  return {
    customerIds: command.customerIds,
    toolId: command.toolId,
    certifiedBy: command.certifiedBy,
  };
}
