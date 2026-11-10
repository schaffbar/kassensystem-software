import { CertificationRequirement } from '../shared/models/certification-requirement';
import { ToolArea } from '../shared/models/tool-area';

export interface Tool {
  id: string;
  name: string;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}
