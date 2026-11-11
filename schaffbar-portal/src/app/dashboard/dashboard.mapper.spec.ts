import { describe, expect, it } from 'vitest';

import { customerFixture } from '../shared/api/customers/customer.api-fixtures';
import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { AgeGroup } from '../shared/models/age-group';
import { ToolArea } from '../shared/models/tool-area';
import { toActiveTool, toActiveUser, toRfidReader, toTool, toUser } from './dashboard.mapper';

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

  describe('toTool', () => {
    it('should take name, area and hardware from the API model', () => {
      expect(
        toTool(toolFixture({ id: 'tool-1', name: 'Bandsäge', rfidReaderId: 'reader-1', ipAddress: '10.0.0.1' })),
      ).toEqual({
        id: 'tool-1',
        name: 'Bandsäge',
        area: ToolArea.Holz,
        rfidReaderId: 'reader-1',
        wlanRelaisType: null,
      });
    });
  });

  describe('toUser', () => {
    it('should take only age group and membership besides the id', () => {
      expect(toUser(customerFixture({ ageGroup: AgeGroup.Under18, clubMember: true }))).toEqual({
        id: 'user-1',
        ageGroup: AgeGroup.Under18,
        clubMember: true,
      });
    });
  });

  describe('toRfidReader', () => {
    it('should take name, MAC address and type', () => {
      expect(
        toRfidReader({
          id: 'reader-1',
          macAddress: 'AA:BB',
          type: null,
          name: null,
          socketName: 'socket-1',
          createdAt: '2026-01-01T10:00:00Z',
        }),
      ).toEqual({ id: 'reader-1', name: null, macAddress: 'AA:BB', type: null });
    });
  });
});
