import { UserToolCertificationRowViewModel } from '../../components/user-tool-certifications/user-tool-certifications.component';
import { ToolCertification } from '../../tool-certification.model';
import { Tool } from '../../tool.model';

// the overview and the certifications tab show the same rows – tool and instructor joined by their ids
export function toUserToolCertificationRowViewModels(
  certifications: ToolCertification[],
  tools: Tool[],
  userNames: Map<string, string>,
): UserToolCertificationRowViewModel[] {
  const toolsById = new Map(tools.map((tool) => [tool.id, tool]));
  return certifications.map((certification) => {
    const tool = toolsById.get(certification.toolId);
    return {
      customerId: certification.customerId,
      toolId: certification.toolId,
      toolName: tool?.name ?? null,
      area: tool?.area ?? null,
      certificationRequirement: tool?.certificationRequirement ?? null,
      certifierName: certification.certifiedBy ? (userNames.get(certification.certifiedBy) ?? null) : null,
      status: certification.status,
      certifiedAt: certification.certifiedAt,
    };
  });
}
