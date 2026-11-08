import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import {
  RfidReaderOptionViewModel,
  ToolRfidReaderAssignmentComponent,
  ToolRfidReaderAssignmentViewModel,
} from './tool-rfid-reader-assignment.component';

describe('ToolRfidReaderAssignmentComponent', () => {
  const rfidReaders: RfidReaderOptionViewModel[] = [
    { id: 'reader-1', name: 'Reader 1', macAddress: 'AA:BB' },
    { id: 'reader-2', name: 'Reader 2', macAddress: 'CC:DD' },
    { id: 'reader-3', name: null, macAddress: 'EE:FF' },
  ];

  function createTool(rfidReaderId: string | null, canClearRfidReader = true): ToolRfidReaderAssignmentViewModel {
    return { id: 'tool-1', name: 'Tool 1', rfidReaderId, canClearRfidReader };
  }

  let fixture: ComponentFixture<ToolRfidReaderAssignmentComponent>;
  let loader: HarnessLoader;
  let changed: Mock;
  let cleared: Mock;
  let openConfirmActionDialog: Mock;

  async function setup(tool: ToolRfidReaderAssignmentViewModel): Promise<void> {
    openConfirmActionDialog = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ToolRfidReaderAssignmentComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ConfirmActionDialogService, useValue: { open: openConfirmActionDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolRfidReaderAssignmentComponent);
    fixture.componentRef.setInput('toolRfidReaderAssignment', tool);
    fixture.componentRef.setInput('rfidReaders', rfidReaders);
    changed = vi.fn();
    cleared = vi.fn();
    fixture.componentInstance.rfidReaderChanged.subscribe(changed);
    fixture.componentInstance.rfidReaderCleared.subscribe(cleared);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  }

  async function selectedText(): Promise<string> {
    return normalize(await (await loader.getHarness(MatSelectHarness)).getValueText());
  }

  async function selectReader(text: RegExp): Promise<void> {
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text });
    await fixture.whenStable();
  }

  function buttonTexts(): string[] {
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.actions button'));
    return buttons.map((button) => button.textContent!.trim());
  }

  async function clearButton(): Promise<MatButtonHarness> {
    return loader.getHarness(MatButtonHarness.with({ selector: '.clear-button' }));
  }

  function blockedHint(): HTMLElement | null {
    return fixture.nativeElement.querySelector('.blocked-hint');
  }

  describe('tool with switch box', () => {
    beforeEach(() => setup(createTool('reader-1')));

    it('should preselect the switch box of the tool and offer only to disconnect it', async () => {
      expect(await selectedText()).toBe('Reader 1 · AA:BB');
      expect(buttonTexts()).toEqual(['tools.pages.toolDetails.hardwareConfiguration.clearSwitchBox']);
      expect(blockedHint()).toBeNull();
    });

    it('should show the name with the MAC address, or only the MAC address of a switch box without name', async () => {
      const select = await loader.getHarness(MatSelectHarness);
      await select.open();

      const texts = await Promise.all((await select.getOptions()).map((option) => option.getText()));
      expect(texts.map(normalize)).toEqual(['Reader 1 · AA:BB', 'Reader 2 · CC:DD', 'EE:FF']);
    });

    it('should emit the change command on save', async () => {
      await selectReader(/Reader 2/);
      await (await loader.getHarness(MatButtonHarness.with({ selector: '[type="submit"]' }))).click();

      expect(changed).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', rfidReaderId: 'reader-2' });
    });

    it('should restore the switch box of the tool on cancel', async () => {
      await selectReader(/Reader 2/);
      await (await loader.getHarness(MatButtonHarness.with({ text: 'shared.actions.cancel' }))).click();
      await fixture.whenStable();

      expect(await selectedText()).toBe('Reader 1 · AA:BB');
      expect(buttonTexts()).toEqual(['tools.pages.toolDetails.hardwareConfiguration.clearSwitchBox']);
    });

    it('should take over the switch box when the tool input changes', async () => {
      await selectReader(/EE:FF/);

      fixture.componentRef.setInput('toolRfidReaderAssignment', createTool('reader-2'));
      await fixture.whenStable();

      expect(await selectedText()).toBe('Reader 2 · CC:DD');
      expect(changed).not.toHaveBeenCalled();
    });

    it('should emit the clear command after confirmation', async () => {
      openConfirmActionDialog.mockReturnValue(of(true));

      await (await clearButton()).click();

      expect(openConfirmActionDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'tools.dialogs.clearRfidReader.title',
        message: 'tools.dialogs.clearRfidReader.message',
        entity: 'Tool 1',
        confirmLabel: 'tools.dialogs.clearRfidReader.confirm',
        destructive: true,
      });
      expect(cleared).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1' });
    });

    it('should not emit the clear command when cancelled', async () => {
      openConfirmActionDialog.mockReturnValue(of(false));

      await (await clearButton()).click();

      expect(cleared).not.toHaveBeenCalled();
    });
  });

  describe('running tool', () => {
    beforeEach(() => setup(createTool('reader-1', false)));

    it('should not allow to disconnect the switch box and explain why', async () => {
      expect(await (await clearButton()).isDisabled()).toBe(true);
      expect(blockedHint()?.textContent?.trim()).toBe(
        'tools.pages.toolDetails.hardwareConfiguration.clearSwitchBoxBlocked',
      );
    });
  });

  describe('tool without switch box', () => {
    beforeEach(() => setup(createTool(null)));

    it('should show no actions until a switch box is selected', async () => {
      expect(buttonTexts()).toEqual([]);

      await selectReader(/EE:FF/);

      expect(buttonTexts()).toEqual(['shared.actions.cancel', 'shared.actions.save']);
    });
  });

  describe('no free switch box', () => {
    beforeEach(async () => {
      await setup(createTool(null));
      fixture.componentRef.setInput('rfidReaders', []);
      await fixture.whenStable();
    });

    it('should disable the selection and explain why it is empty', async () => {
      expect(await (await loader.getHarness(MatSelectHarness)).isDisabled()).toBe(true);
      expect(fixture.nativeElement.querySelector('mat-hint').textContent.trim()).toBe(
        'tools.pages.toolDetails.hardwareConfiguration.noFreeSwitchBox',
      );
    });
  });
});

function normalize(text: string): string {
  return text.replace(/\s+/g, ' ').trim();
}
