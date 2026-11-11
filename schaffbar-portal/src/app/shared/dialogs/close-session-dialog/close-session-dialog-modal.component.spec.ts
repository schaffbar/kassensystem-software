import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogRef } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { CloseSessionDialogModalComponent, CloseSessionDialogViewModel } from './close-session-dialog-modal.component';

registerLocaleData(localeDe);

describe('CloseSessionDialogModalComponent', () => {
  const at = (hours: number, minutes: number) => new Date(2026, 10, 9, hours, minutes);
  // checked in 14:05–16:30, band saw 14:12–14:31, a deleted tool 15:00–15:07
  const viewModel: CloseSessionDialogViewModel = {
    startTime: at(14, 5),
    workshop: { startTime: at(14, 5), endTime: at(16, 30), minutes: 145, units: 13 },
    tools: [
      { toolId: 'tool-1', name: 'Bandsäge', startTime: at(14, 12), endTime: at(14, 31), minutes: 19, units: 6 },
      { toolId: 'tool-2', name: null, startTime: at(15, 0), endTime: at(15, 7), minutes: 7, units: 2 },
    ],
    hasRunningUsage: false,
  };
  const confirmText = /shared.dialogs.closeSession.actions.confirm/;
  const cancelText = 'shared.actions.cancel';

  let fixture: ComponentFixture<CloseSessionDialogModalComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    // the times of the test data are of "today" – only the clock, the timers stay real
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 10, 9, 22, 0) });
    await TestBed.configureTestingModule({
      imports: [CloseSessionDialogModalComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: LOCALE_ID, useValue: 'de-DE' },
        // the close button of the header
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CloseSessionDialogModalComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.componentRef.setInput('userName', 'Max Mustermann');
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

  afterEach(() => vi.useRealTimers());

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  function cells(selector: string): string[][] {
    const rows: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll(selector));
    return rows.map((row) =>
      Array.from(row.children).map((cell) => cell.textContent?.replace(/\s+/g, ' ').trim() ?? ''),
    );
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

  it('should name the person and the check-in time in the header', async () => {
    await render({ viewModel });

    expect(query('schbar-form-dialog-header')?.textContent).toContain('shared.dialogs.closeSession.subtitle');
  });

  it('should show workshop and tools with time, duration and units, with a dash for a deleted tool', async () => {
    await render({ viewModel });

    expect(cells('tr.workshop-total')).toEqual([
      ['shared.dialogs.closeSession.workshop', '14:05 – 16:30', 'shared.dialogs.closeSession.minutes', '13'],
    ]);
    expect(cells('tr.tool-total')).toEqual([
      ['Bandsäge', '14:12 – 14:31', 'shared.dialogs.closeSession.minutes', '6'],
      ['–', '15:00 – 15:07', 'shared.dialogs.closeSession.minutes', '2'],
    ]);
    expect(query('.message')).not.toBeNull();
    expect(await isConfirmDisabled()).toBe(false);
  });

  it('should show a hint and lock closing while a usage is running', async () => {
    await render({
      viewModel: { ...viewModel, workshop: { ...viewModel.workshop, endTime: null }, hasRunningUsage: true },
    });

    expect(query('.running-usage')?.textContent).toContain('shared.dialogs.closeSession.messages.runningUsage');
    expect(cells('tr.workshop-total')[0][1]).toBe('14:05 – shared.dialogs.closeSession.now');
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
