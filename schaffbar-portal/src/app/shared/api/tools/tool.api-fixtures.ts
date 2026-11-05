import { CertificationRequirement } from '../../models/certification-requirement';
import { ToolArea } from '../../models/tool-area';
import { ToolApiModel } from './tool.api-model';

export function toolFixture(overrides: Partial<ToolApiModel> = {}): ToolApiModel {
  return {
    id: 'tool-1',
    name: 'Bandsäge',
    description: null,
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Yellow,
    rfidReaderId: null,
    wlanRelaisType: null,
    ipAddress: null,
    httpStartCommand: null,
    onCommand: null,
    offCommand: null,
    instructors: [],
    ...overrides,
  };
}
