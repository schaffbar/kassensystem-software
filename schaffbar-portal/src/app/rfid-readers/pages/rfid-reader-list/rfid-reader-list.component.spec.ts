import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { RfidReader } from '../../rfid-reader.model';
import { RfidReadersStore } from '../../rfid-readers.store';
import { RfidReaderListComponent } from './rfid-reader-list.component';

describe('RfidReaderListComponent', () => {
  const rfidReaders: RfidReader[] = [
    {
      id: 'reader-1',
      macAddress: 'AA:BB',
      type: RfidReaderType.SwitchBox,
      name: 'Reader 1',
      socketName: null,
      createdAt: new Date('2026-01-01T10:00:00Z'),
    },
    {
      id: 'reader-2',
      macAddress: 'CC:DD',
      type: null,
      name: null,
      socketName: null,
      createdAt: new Date('2026-01-02T10:00:00Z'),
    },
  ];

  let fixture: ComponentFixture<RfidReaderListComponent>;
  let storeMock: {
    entities: WritableSignal<RfidReader[]>;
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    reloadRfidReaders: Mock;
    deleteRfidReader: Mock;
  };
  let openConfirmDeleteDialog: Mock;

  beforeEach(async () => {
    storeMock = {
      entities: signal(rfidReaders),
      isLoading: signal(false),
      hasLoadError: signal(false),
      reloadRfidReaders: vi.fn(),
      deleteRfidReader: vi.fn(),
    };
    openConfirmDeleteDialog = vi.fn();

    await TestBed.configureTestingModule({
      imports: [RfidReaderListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: RfidReadersStore, useValue: storeMock },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDeleteDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RfidReaderListComponent);
    await fixture.whenStable();
  });

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.trim() ?? '');
  }

  it('should load the RFID readers on init', () => {
    expect(storeMock.reloadRfidReaders).toHaveBeenCalledOnce();
  });

  it('should show a dash for a reader without name and the "none" type for a reader without type', () => {
    expect(texts('td.mat-column-name')).toEqual(['Reader 1', '–']);
    expect(texts('td.mat-column-type')[1]).toContain('rfidReaders.types.none');
  });

  it('should open the detail page of a clicked reader', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.nativeElement.querySelector('tr[mat-row]').click();

    expect(navigate).toHaveBeenCalledExactlyOnceWith(['rfid-readers', 'reader-1']);
  });

  describe('delete', () => {
    function clickDelete(row: number, confirmed: boolean): void {
      openConfirmDeleteDialog.mockReturnValue(of(confirmed));
      fixture.nativeElement.querySelectorAll('tr[mat-row] .danger-button')[row].click();
    }

    it('should delete the reader after confirmation', () => {
      clickDelete(0, true);

      expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'rfidReaders.dialogs.deleteRfidReader.title',
        entity: 'Reader 1',
        message: 'rfidReaders.dialogs.deleteRfidReader.message',
      });
      expect(storeMock.deleteRfidReader).toHaveBeenCalledExactlyOnceWith({ rfidReaderId: 'reader-1' });
    });

    it('should name a reader without name by its MAC address', () => {
      clickDelete(1, true);

      expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ entity: 'CC:DD' }));
    });

    it('should not delete the reader when the dialog is cancelled', () => {
      clickDelete(0, false);

      expect(storeMock.deleteRfidReader).not.toHaveBeenCalled();
    });
  });
});
