import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { ToolWlanRelaisComponent, ToolWlanRelaisViewModel } from './tool-wlan-relais.component';

describe('ToolWlanRelaisComponent', () => {
  let fixture: ComponentFixture<ToolWlanRelaisComponent>;
  let loader: HarnessLoader;
  let updated: Mock;
  let cleared: Mock;
  let openConfirmActionDialog: Mock;

  function createTool(relais?: { wlanRelaisType: WlanRelaisType; ipAddress: string }): ToolWlanRelaisViewModel {
    return { id: 'tool-1', name: 'Tool 1', wlanRelaisType: null, ipAddress: null, ...relais };
  }

  async function setup(tool: ToolWlanRelaisViewModel): Promise<void> {
    openConfirmActionDialog = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ToolWlanRelaisComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ConfirmActionDialogService, useValue: { open: openConfirmActionDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolWlanRelaisComponent);
    fixture.componentRef.setInput('toolWlanRelais', tool);
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
    return (await loader.getHarness(MatFormFieldHarness.with({ selector: '.mono-field' }))).getTextErrors();
  }

  async function click(text: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ text }));
    await button.click();
    await fixture.whenStable();
  }

  function commands(): string[] | null {
    const code: HTMLElement | null = fixture.nativeElement.querySelector('.commands code');
    return code ? Array.from(code.children, (span) => span.textContent!.trim()) : null;
  }

  function buttonTexts(): string[] {
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.actions button'));
    return buttons.map((button) => button.textContent!.trim());
  }

  describe('tool without WLAN relais', () => {
    beforeEach(() => setup(createTool()));

    it('should show only the type selection', async () => {
      expect(await ipInput()).toBeNull();
      expect(commands()).toBeNull();
      expect(buttonTexts()).toEqual([]);
    });

    it('should show the IP address and derive the commands after selecting a type', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await (await ipInput())!.setValue('192.168.0.10');

      expect(commands()).toEqual([
        'tools.pages.toolDetails.hardwareConfiguration.on:',
        'http://192.168.0.10/relay/0?turn=on',
        'tools.pages.toolDetails.hardwareConfiguration.off:',
        'http://192.168.0.10/relay/0?turn=off',
      ]);
      expect(buttonTexts()).toEqual(['shared.actions.cancel', 'shared.actions.save']);
    });

    it('should require the IP address once a type is selected', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await click('shared.actions.save');

      expect(await ipErrors()).toEqual(['shared.errors.required']);
      expect(updated).not.toHaveBeenCalled();
    });

    it('should reject an invalid IP address', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await (await ipInput())!.setValue('192.168.0.300');
      await click('shared.actions.save');

      expect(await ipErrors()).toEqual(['tools.pages.toolDetails.errors.invalidIpAddress']);
      expect(commands()).toBeNull();
      expect(updated).not.toHaveBeenCalled();
    });

    it('should emit the command for a valid configuration', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_PRO');
      await (await ipInput())!.setValue('10.0.0.5');
      await click('shared.actions.save');

      expect(updated).toHaveBeenCalledExactlyOnceWith({
        toolId: 'tool-1',
        wlanRelaisType: WlanRelaisType.ShellyPro,
        ipAddress: '10.0.0.5',
      });
    });

    it('should forget the selected type on cancel', async () => {
      await selectType('tools.wlanRelaisTypes.SHELLY_1');
      await click('shared.actions.cancel');

      expect(await ipInput()).toBeNull();
      expect(buttonTexts()).toEqual([]);
    });
  });

  describe('tool with WLAN relais', () => {
    beforeEach(() => setup(createTool({ wlanRelaisType: WlanRelaisType.Shelly1, ipAddress: '192.168.0.10' })));

    it('should offer only to remove the relais', () => {
      expect(buttonTexts()).toEqual(['tools.pages.toolDetails.hardwareConfiguration.removeRelais']);
    });

    it('should emit the clear command after confirmation', async () => {
      openConfirmActionDialog.mockReturnValue(of(true));

      await click('tools.pages.toolDetails.hardwareConfiguration.removeRelais');

      expect(openConfirmActionDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'tools.dialogs.clearWlanRelais.title',
        message: 'tools.dialogs.clearWlanRelais.message',
        entity: 'Tool 1',
        confirmLabel: 'tools.dialogs.clearWlanRelais.confirm',
        destructive: true,
      });
      expect(cleared).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1' });
    });

    it('should not emit the clear command when cancelled', async () => {
      openConfirmActionDialog.mockReturnValue(of(false));

      await click('tools.pages.toolDetails.hardwareConfiguration.removeRelais');

      expect(cleared).not.toHaveBeenCalled();
    });

    it('should restore the saved values on cancel', async () => {
      await (await ipInput())!.setValue('10.0.0.1');
      await click('shared.actions.cancel');

      expect(await (await ipInput())!.getValue()).toBe('192.168.0.10');
      expect(buttonTexts()).toEqual(['tools.pages.toolDetails.hardwareConfiguration.removeRelais']);
    });

    it('should take over the values when the tool input changes', async () => {
      await (await ipInput())!.setValue('10.0.0.1');

      fixture.componentRef.setInput(
        'toolWlanRelais',
        createTool({ wlanRelaisType: WlanRelaisType.Shelly2, ipAddress: '172.16.0.1' }),
      );
      await fixture.whenStable();

      expect(await (await ipInput())!.getValue()).toBe('172.16.0.1');
      expect(updated).not.toHaveBeenCalled();
    });
  });
});
