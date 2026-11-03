import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CloseSessionDialogModalComponent, CloseSessionViewModel } from './close-session-dialog-modal.component';

describe('CloseSessionDialogModalComponent', () => {
  const viewModel: CloseSessionViewModel = {
    customerId: 'user-1',
    workshop: { minutes: 75, units: 13 },
    tools: [
      { toolId: 'tool-1', name: 'Bandsäge', minutes: 31, units: 6 },
      { toolId: 'tool-2', name: null, minutes: 7, units: 2 },
    ],
    hasRunningUsage: false,
  };
  const confirmText = 'users.dialogs.closeSession.actions.confirm';
  const cancelText = 'users.dialogs.closeSession.actions.cancel';

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
    viewModel: CloseSessionViewModel | null;
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

    expect(cells('tr.workshop-total')).toEqual([['users.dialogs.closeSession.workshopTime', '75', '13']]);
    expect(cells('tr.tool-total')).toEqual([
      ['Bandsäge', '31', '6'],
      ['–', '7', '2'],
    ]);
    expect(await isConfirmDisabled()).toBe(false);
  });

  it('should show a hint instead of the totals while a usage is running', async () => {
    await render({ viewModel: { ...viewModel, hasRunningUsage: true } });

    expect(query('.running-usage')?.textContent).toContain('users.pages.openSession.messages.runningUsage');
    expect(query('table')).toBeNull();
    expect(await isConfirmDisabled()).toBe(true);
  });

  it('should show a hint when there is no open session', async () => {
    await render({ viewModel: null });

    expect(query('.hint')?.textContent).toContain('users.pages.openSession.messages.noOpenSession');
    expect(await isConfirmDisabled()).toBe(true);
  });

  it('should show an error when loading failed', async () => {
    await render({ hasLoadError: true, viewModel: null });

    expect(query('.load-error')?.textContent).toContain('shared.errors.loadFailed');
    expect(await isConfirmDisabled()).toBe(true);
  });

  it('should emit the command on confirm', async () => {
    const closeConfirmed = vi.fn();
    fixture.componentInstance.closeConfirmed.subscribe(closeConfirmed);
    await render({ viewModel });

    await (await loader.getHarness(MatButtonHarness.with({ text: confirmText }))).click();

    expect(closeConfirmed).toHaveBeenCalledExactlyOnceWith({ customerId: 'user-1' });
  });

  it('should emit on cancel', async () => {
    const cancelled = vi.fn();
    fixture.componentInstance.cancelled.subscribe(cancelled);
    await render({ viewModel });

    await (await loader.getHarness(MatButtonHarness.with({ text: cancelText }))).click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
