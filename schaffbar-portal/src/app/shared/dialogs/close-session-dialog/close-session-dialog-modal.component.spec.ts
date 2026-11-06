import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CloseSessionDialogModalComponent, CloseSessionDialogViewModel } from './close-session-dialog-modal.component';

describe('CloseSessionDialogModalComponent', () => {
  const viewModel: CloseSessionDialogViewModel = {
    workshop: { minutes: 75, units: 13 },
    tools: [
      { toolId: 'tool-1', name: 'Bandsäge', minutes: 31, units: 6 },
      { toolId: 'tool-2', name: null, minutes: 7, units: 2 },
    ],
    hasRunningUsage: false,
  };
  const confirmText = 'shared.dialogs.closeSession.actions.confirm';
  const cancelText = 'shared.dialogs.closeSession.actions.cancel';

  let fixture: ComponentFixture<CloseSessionDialogModalComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CloseSessionDialogModalComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(CloseSessionDialogModalComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  async function render(inputs: {
    isLoading?: boolean;
    hasLoadError?: boolean;
    viewModel: CloseSessionDialogViewModel | null;
  }): Promise<void> {
    fixture.componentRef.setInput('isLoading', inputs.isLoading ?? false);
    fixture.componentRef.setInput('hasLoadError', inputs.hasLoadError ?? false);
    fixture.componentRef.setInput('viewModel', inputs.viewModel);
    await fixture.whenStable();
  }

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  function cells(selector: string): string[][] {
    const rows: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll(selector));
    return rows.map((row) => Array.from(row.children).map((cell) => cell.textContent?.trim() ?? ''));
  }

  async function isConfirmDisabled(): Promise<boolean> {
    return (await loader.getHarness(MatButtonHarness.with({ text: confirmText }))).isDisabled();
  }

  it('should show only a spinner while loading', async () => {
    await render({ isLoading: true, viewModel });

    expect(query('mat-spinner')).not.toBeNull();
    expect(query('table')).toBeNull();
    expect(await isConfirmDisabled()).toBe(true);
  });

  it('should show the workshop totals and the totals of each tool, with a dash for a deleted tool', async () => {
    await render({ viewModel });

    expect(cells('tr.workshop-total')).toEqual([['shared.dialogs.closeSession.workshopTime', '75', '13']]);
    expect(cells('tr.tool-total')).toEqual([
      ['Bandsäge', '31', '6'],
      ['–', '7', '2'],
    ]);
    expect(await isConfirmDisabled()).toBe(false);
  });

  it('should show a hint instead of the totals while a usage is running', async () => {
    await render({ viewModel: { ...viewModel, hasRunningUsage: true } });

    expect(query('.running-usage')?.textContent).toContain('shared.dialogs.closeSession.messages.runningUsage');
    expect(query('table')).toBeNull();
    expect(await isConfirmDisabled()).toBe(true);
  });

  it('should show a hint when there is no open session', async () => {
    await render({ viewModel: null });

    expect(query('.hint')?.textContent).toContain('shared.dialogs.closeSession.messages.noOpenSession');
    expect(await isConfirmDisabled()).toBe(true);
  });

  it('should show an error when loading failed', async () => {
    await render({ hasLoadError: true, viewModel: null });

    expect(query('.load-error')?.textContent).toContain('shared.errors.loadFailed');
    expect(await isConfirmDisabled()).toBe(true);
  });

  it('should emit on confirm', async () => {
    const confirmed = vi.fn();
    fixture.componentInstance.confirmed.subscribe(confirmed);
    await render({ viewModel });

    await (await loader.getHarness(MatButtonHarness.with({ text: confirmText }))).click();

    expect(confirmed).toHaveBeenCalledOnce();
  });

  it('should emit on cancel', async () => {
    const cancelled = vi.fn();
    fixture.componentInstance.cancelled.subscribe(cancelled);
    await render({ viewModel });

    await (await loader.getHarness(MatButtonHarness.with({ text: cancelText }))).click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
