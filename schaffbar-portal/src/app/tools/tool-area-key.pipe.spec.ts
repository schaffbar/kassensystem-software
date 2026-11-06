import { describe, expect, it } from 'vitest';

import { ToolArea } from '../shared/models/tool-area';
import { ToolAreaKeyPipe } from './tool-area-key.pipe';

describe('ToolAreaKeyPipe', () => {
  it('should return the translation key of the area', () => {
    expect(new ToolAreaKeyPipe().transform(ToolArea.FabLab)).toBe('tools.areas.FABLAB');
  });
});
