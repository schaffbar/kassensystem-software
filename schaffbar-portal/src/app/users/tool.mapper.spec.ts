import { describe, expect, it } from 'vitest';

import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { CertificationRequirement } from '../shared/models/certification-requirement';
import { ToolArea } from '../shared/models/tool-area';
import { toTool } from './tool.mapper';

describe('toTool', () => {
  it('should take only id, name, area and requirement from the API model', () => {
    expect(
      toTool(
        toolFixture({
          id: 'tool-1',
          name: 'Bandsäge',
          area: ToolArea.Holz,
          certificationRequirement: CertificationRequirement.Red,
          ipAddress: '10.0.0.1',
        }),
      ),
    ).toEqual({
      id: 'tool-1',
      name: 'Bandsäge',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    });
  });
});
