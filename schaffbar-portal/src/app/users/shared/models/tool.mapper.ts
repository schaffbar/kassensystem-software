import { ToolApiModel } from '../../../shared/api/tools/tool.api-model';
import { Tool } from './tool.model';

export function toTool(apiModel: ToolApiModel): Tool {
  return {
    id: apiModel.id,
    name: apiModel.name,
  };
}
