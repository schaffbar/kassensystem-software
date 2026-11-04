import { describe, expect, it } from 'vitest';

import { toActiveTool, toActiveUser } from './dashboard.mapper';

describe('dashboard.mapper', () => {
  describe('toActiveUser', () => {
    it('should map the API model and parse the entry time', () => {
      expect(
        toActiveUser({
          customerId: 'user-1',
          firstName: 'Max',
          lastName: 'Mustermann',
          entryTime: '2026-10-27T08:30:00Z',
        }),
      ).toEqual({
        customerId: 'user-1',
        firstName: 'Max',
        lastName: 'Mustermann',
        entryTime: new Date('2026-10-27T08:30:00Z'),
      });
    });
  });

  describe('toActiveTool', () => {
    it('should map the API model', () => {
      const apiModel = {
        toolId: 'tool-1',
        toolName: 'Bandsäge',
        customerId: 'user-1',
        firstName: 'Max',
        lastName: 'M',
      };

      expect(toActiveTool(apiModel)).toEqual(apiModel);
    });
  });
});
