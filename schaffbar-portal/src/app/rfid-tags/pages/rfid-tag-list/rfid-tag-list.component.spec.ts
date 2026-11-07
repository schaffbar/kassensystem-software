import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { RfidTag } from '../../rfid-tag.model';
import { RfidTagsStore } from '../../rfid-tags.store';
import { RfidTagListComponent } from './rfid-tag-list.component';

describe('RfidTagListComponent', () => {
  const rfidTags: RfidTag[] = [
    { id: 'tag-1', active: true },
    { id: 'tag-2', active: false },
  ];

  let fixture: ComponentFixture<RfidTagListComponent>;
  let openConfirmDeleteDialog: Mock;
  let storeMock: {
    rfidTags: WritableSignal<RfidTag[]>;
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    reloadRfidTags: Mock;
    deleteRfidTag: Mock;
  };

  beforeEach(async () => {
    openConfirmDeleteDialog = vi.fn();
    storeMock = {
      rfidTags: signal(rfidTags),
      isLoading: signal(false),
      hasLoadError: signal(false),
      reloadRfidTags: vi.fn(),
      deleteRfidTag: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [RfidTagListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: RfidTagsStore, useValue: storeMock },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDeleteDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RfidTagListComponent);
    await fixture.whenStable();
  });

  it('should load RFID tags on init', () => {
    expect(storeMock.reloadRfidTags).toHaveBeenCalledTimes(1);
  });

  it('should show the number of RFID tags in the header', () => {
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('(2)');
  });

  it('should render one table row per RFID tag', () => {
    const rows = fixture.nativeElement.querySelectorAll('tr[mat-row]');

    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('tag-1');
    expect(rows[1].textContent).toContain('tag-2');
  });

  it('should reload RFID tags when the refresh button is clicked', () => {
    storeMock.reloadRfidTags.mockClear();
    fixture.nativeElement.querySelector('.rfid-tag-list-header button').click();

    expect(storeMock.reloadRfidTags).toHaveBeenCalledTimes(1);
  });

  describe('delete', () => {
    function clickDeleteOfFirstRow(confirmed: boolean): void {
      openConfirmDeleteDialog.mockReturnValue(of(confirmed));

      fixture.nativeElement.querySelector('tr[mat-row] button').click();
    }

    it('should delete the RFID tag after confirmation', () => {
      clickDeleteOfFirstRow(true);

      expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'rfidTags.dialogs.deleteRfidTag.title',
        entity: 'tag-1',
        message: 'rfidTags.dialogs.deleteRfidTag.message',
        confirmLabel: 'rfidTags.dialogs.deleteRfidTag.confirm',
      });
      expect(storeMock.deleteRfidTag).toHaveBeenCalledExactlyOnceWith({ rfidTagId: 'tag-1' });
    });

    it('should not delete the RFID tag when the dialog is cancelled', () => {
      clickDeleteOfFirstRow(false);

      expect(storeMock.deleteRfidTag).not.toHaveBeenCalled();
    });
  });
});
