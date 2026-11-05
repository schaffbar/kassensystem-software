import { describe, expect, it } from 'vitest';

import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { toTool } from './tool.mapper';

describe('toTool', () => {
  it('should take only id and name from the API model', () => {
    expect(toTool(toolFixture({ id: 'tool-1', name: 'Bandsäge' }))).toEqual({ id: 'tool-1', name: 'Bandsäge' });
  });
});
