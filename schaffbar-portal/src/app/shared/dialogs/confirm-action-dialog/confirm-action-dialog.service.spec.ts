import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MatDialog } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { ConfirmActionDialogModalComponent } from './confirm-action-dialog-modal.component';
import { ConfirmActionDialogService } from './confirm-action-dialog.service';

describe('ConfirmActionDialogService', () => {
  let service: ConfirmActionDialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    });
    service = TestBed.inject(ConfirmActionDialogService);
  });

  afterEach(() => TestBed.inject(MatDialog).closeAll());

  function open(): Promise<boolean> {
    const result = firstValueFrom(
      service.open({ title: 'pause.title', message: 'm', confirmLabel: 'c', destructive: false }),
    );
    TestBed.tick();
    return result;
  }

  function modalButtons(): HTMLButtonElement[] {
    return Array.from(document.querySelectorAll('schbar-confirm-action-dialog-modal button'));
  }

  it('should open the modal with the view model', async () => {
    void open();

    const [dialogRef] = TestBed.inject(MatDialog).openDialogs;
    expect(dialogRef.componentInstance).toBeInstanceOf(ConfirmActionDialogModalComponent);
    expect(dialogRef.disableClose).toBe(true);
    expect(document.querySelector('schbar-confirm-action-dialog-modal h2')?.textContent).toContain('pause.title');
  });

  it('should emit true on confirm', async () => {
    const result = open();

    modalButtons()[1].click();

    expect(await result).toBe(true);
  });

  it('should emit false on cancel', async () => {
    const result = open();

    modalButtons()[0].click();

    expect(await result).toBe(false);
  });
});
