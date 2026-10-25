import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement, Tool, ToolArea, WlanRelaisType } from '../../tool.model';
import { ToolWlanRelaisComponent } from './tool-wlan-relais.component';

describe('ToolWlanRelaisComponent', () => {
  let fixture: ComponentFixture<ToolWlanRelaisComponent>;
  let loader: HarnessLoader;
  let updated: Mock;
  let cleared: Mock;

  function createTool(relais?: { wlanRelaisType: WlanRelaisType; ipAddress: string }): Tool {
    return {
      id: 'tool-1',
      name: 'Tool 1',
      description: '',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Yellow,
      createdAt: new Date(),
      updatedAt: new Date(),
      ...relais,
    };
  }

  async function setup(tool: Tool): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ToolWlanRelaisComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolWlanRelaisComponent);
    fixture.componentRef.setInput('tool', tool);
    updated = vi.fn();
    cleared = vi.fn();
    fixture.componentInstance.wlanRelaisUpdated.subscribe(updated);
    fixture.componentInstance.wlanRelaisCleared.subscribe(cleared);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  }

  async function selectType(text: string): Promise<void> {
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text });
  }

  async function ipInput(): Promise<MatInputHarness | null> {
    return loader.getHarnessOrNull(MatInputHarness.with({ selector: '#ipAddress' }));
  }

  async function ipErrors(): Promise<string[]> {
    const field = await loader.getHarness(
      MatFormFieldHarness.with({ floatingLabelText: 'tools.shared.toolProperties.ipAddress' }),
    );
    return field.getTextErrors();
  }

  async function click(text: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ text }));
    await button.click();
    await fixture.whenStable();
  }

  function httpStartCommand(): string {
    return fixture.nativeElement.querySelector('.readonly-field input').value;
  }

  function actionButtonCount(): number {
    return fixture.nativeElement.querySelectorAll('.wlan-relais-actions button').length;
  }

  describe('tool without WLAN relais', () => {
    beforeEach(() => setup(createTool()));

    it('should hide the IP address and actions', async () => {
      expect(await ipInput()).toBeNull();
      expect(actionButtonCount()).toBe(0);
    });

    it('should show the IP address and derive the start command after selecting a type', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await (await ipInput())!.setValue('192.168.0.10');

      expect(httpStartCommand()).toBe('http://192.168.0.10/relay/0?');
      expect(actionButtonCount()).toBe(2);
    });

    it('should not be dirty after selecting a type, entering an IP address and switching back to "--"', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await (await ipInput())!.setValue('192.168.0.10');
      await selectType('--');

      expect(actionButtonCount()).toBe(0);
    });

    it('should keep the entered IP address when switching to "--" and back', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await (await ipInput())!.setValue('192.168.0.10');
      await selectType('--');
      await selectType('tools.wlanRelaisTypes.SHELLY_2');

      expect(await (await ipInput())!.getValue()).toBe('192.168.0.10');
    });

    it('should require the IP address once a type is selected', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await click('shared.dialogs.actions.edit.confirm');

      expect(await ipErrors()).toEqual(['shared.errors.required']);
      expect(updated).not.toHaveBeenCalled();
    });

    it('should reject an invalid IP address', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await (await ipInput())!.setValue('192.168.0.300');
      await click('shared.dialogs.actions.edit.confirm');

      expect(await ipErrors()).toEqual(['tools.pages.toolDetails.errors.invalidIpAddress']);
      expect(updated).not.toHaveBeenCalled();
    });

    it('should emit the command for a valid configuration', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_PRO');
      await (await ipInput())!.setValue('10.0.0.5');
      await click('shared.dialogs.actions.edit.confirm');

      expect(updated).toHaveBeenCalledWith({
        toolId: 'tool-1',
        wlanRelaisType: WlanRelaisType.ShellyPro,
        ipAddress: '10.0.0.5',
      });
    });
  });

  describe('tool with WLAN relais', () => {
    beforeEach(() => setup(createTool({ wlanRelaisType: WlanRelaisType.Shelly1, ipAddress: '192.168.0.10' })));

    it('should emit cleared when the type is removed, even with an invalid IP address', async () => {
      await (await ipInput())!.setValue('invalid');
      await selectType('--');

      expect(await ipInput()).toBeNull();
      await click('shared.dialogs.actions.edit.confirm');

      expect(cleared).toHaveBeenCalledWith({ toolId: 'tool-1' });
      expect(updated).not.toHaveBeenCalled();
    });

    it('should restore the saved values on cancel', async () => {
      await (await ipInput())!.setValue('10.0.0.1');
      await click('shared.dialogs.actions.edit.cancel');

      expect(await (await ipInput())!.getValue()).toBe('192.168.0.10');
      expect(actionButtonCount()).toBe(0);
    });

    it('should take over the values when the tool input changes', async () => {
      await (await ipInput())!.setValue('10.0.0.1');

      fixture.componentRef.setInput(
        'tool',
        createTool({ wlanRelaisType: WlanRelaisType.Shelly2, ipAddress: '172.16.0.1' }),
      );
      await fixture.whenStable();

      expect(await (await ipInput())!.getValue()).toBe('172.16.0.1');
      expect(actionButtonCount()).toBe(0);
    });
  });
});
