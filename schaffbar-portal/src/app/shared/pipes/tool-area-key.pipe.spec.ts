import { describe, expect, it } from 'vitest';

import { ToolArea } from '../models/tool-area';
import { ToolAreaKeyPipe } from './tool-area-key.pipe';

describe('ToolAreaKeyPipe', () => {
  it('should return the translation key of the area', () => {
    expect(new ToolAreaKeyPipe().transform(ToolArea.FabLab)).toBe('shared.toolAreas.FABLAB');
  });
});
