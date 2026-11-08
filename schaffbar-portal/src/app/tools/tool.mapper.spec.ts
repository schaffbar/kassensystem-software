import { describe, expect, it } from 'vitest';

import { toolFixture } from '../shared/api/tools/tool.api-fixtures';
import { CertificationRequirement } from '../shared/models/certification-requirement';
import { ToolArea } from '../shared/models/tool-area';
import { WlanRelaisType } from '../shared/models/wlan-relais-type';
import {
  toCreateToolRequest,
  toInstructorsRequest,
  toSetWlanRelaisRequest,
  toTool,
  toToolListItems,
  toUpdateToolRequest,
} from './tool.mapper';

describe('tool.mapper', () => {
  describe('toTool', () => {
    it('should map every field of the API model', () => {
      const apiModel = {
        id: 'tool-1',
        name: 'Bandsäge',
        description: null,
        area: ToolArea.Holz,
        certificationRequirement: CertificationRequirement.Red,
        rfidReaderId: 'reader-1',
        wlanRelaisType: WlanRelaisType.ShellyPro,
        ipAddress: '192.168.0.10',
        httpStartCommand: 'http://192.168.0.10/rpc/Switch.Set?',
        onCommand: 'id=0&on=true',
        offCommand: 'id=0&on=false',
        instructors: ['user-1'],
      };

      expect(toTool(apiModel)).toEqual(apiModel);
    });
  });

  describe('toToolListItems', () => {
    it('should add the person who uses a tool right now', () => {
      const saw = toolFixture({ id: 'tool-1', name: 'Bandsäge' });
      const lathe = toolFixture({ id: 'tool-2', name: 'Drechselbank' });

      const items = toToolListItems(
        [saw, lathe],
        [{ toolId: 'tool-2', toolName: 'Drechselbank', customerId: 'user-1', firstName: 'Anna', lastName: 'Becker' }],
      );

      expect(items).toEqual([
        { ...toTool(saw), usedBy: null },
        { ...toTool(lathe), usedBy: 'Anna Becker' },
      ]);
    });
  });

  describe('toCreateToolRequest', () => {
    it('should send a missing description as null', () => {
      expect(
        toCreateToolRequest({
          name: 'Bandsäge',
          area: ToolArea.Holz,
          certificationRequirement: CertificationRequirement.Red,
        }),
      ).toEqual({
        name: 'Bandsäge',
        description: null,
        area: ToolArea.Holz,
        certificationRequirement: CertificationRequirement.Red,
      });
    });
  });

  describe('toUpdateToolRequest', () => {
    it('should send the fields without the id', () => {
      expect(
        toUpdateToolRequest({
          toolId: 'tool-1',
          name: 'Bandsäge',
          description: 'groß',
          area: ToolArea.Holz,
          certificationRequirement: CertificationRequirement.Yellow,
        }),
      ).toEqual({
        name: 'Bandsäge',
        description: 'groß',
        area: ToolArea.Holz,
        certificationRequirement: CertificationRequirement.Yellow,
      });
    });
  });

  describe('toSetWlanRelaisRequest', () => {
    it('should send type and ip address without the tool id', () => {
      expect(
        toSetWlanRelaisRequest({ toolId: 'tool-1', wlanRelaisType: WlanRelaisType.Shelly1, ipAddress: '192.168.0.10' }),
      ).toEqual({ wlanRelaisType: WlanRelaisType.Shelly1, ipAddress: '192.168.0.10' });
    });
  });

  describe('toInstructorsRequest', () => {
    it('should send only the instructor ids', () => {
      expect(toInstructorsRequest({ toolId: 'tool-1', instructorIds: ['user-1', 'user-2'] })).toEqual({
        instructorIds: ['user-1', 'user-2'],
      });
    });
  });
});
