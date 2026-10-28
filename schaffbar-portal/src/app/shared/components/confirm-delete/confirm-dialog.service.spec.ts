import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MatDialog } from '@angular/material/dialog';

import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteComponent } from './confirm-delete.component';
import { ConfirmDialogService } from './confirm-dialog.service';

describe('ConfirmDialogService', () => {
  let open: Mock;
  let service: ConfirmDialogService;

  function closeDialogWith(result: boolean | undefined): void {
    open.mockReturnValue({ afterClosed: () => of(result) });
  }

  beforeEach(() => {
    open = vi.fn();
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), { provide: MatDialog, useValue: { open } }],
    });
    service = TestBed.inject(ConfirmDialogService);
  });

  it('should open the confirm dialog with the data and the common options', () => {
    closeDialogWith(true);
    const data = {
      title: 'tools.dialogs.deleteTool.title',
      message: 'tools.dialogs.deleteTool.message',
      entity: 'Säge',
    };

    service.confirm(data).subscribe();

    expect(open).toHaveBeenCalledExactlyOnceWith(ConfirmDeleteComponent, {
      data,
      minWidth: '600px',
      disableClose: true,
    });
  });

  it.each([
    [true, true],
    [undefined, false],
  ])('should emit %s → %s', (dialogResult, expected) => {
    closeDialogWith(dialogResult);
    const confirmed = vi.fn();

    service.confirm({ title: 't', message: 'm' }).subscribe(confirmed);

    expect(confirmed).toHaveBeenCalledExactlyOnceWith(expected);
  });
});
