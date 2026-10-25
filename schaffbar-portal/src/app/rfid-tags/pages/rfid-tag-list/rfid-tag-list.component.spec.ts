import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidTag } from '../../rfid-tag.model';
import { RfidTagStore } from '../../rfid-tags.store';
import { RfidTagListComponent } from './rfid-tag-list.component';

describe('RfidTagListComponent', () => {
  const rfidTags: RfidTag[] = [
    { id: 'tag-1', active: true },
    { id: 'tag-2', active: false },
  ];

  let fixture: ComponentFixture<RfidTagListComponent>;
  let storeMock: {
    entities: WritableSignal<RfidTag[]>;
    loadAllRfidTags: Mock;
    deleteRfidTag: Mock;
  };

  beforeEach(async () => {
    storeMock = {
      entities: signal(rfidTags),
      loadAllRfidTags: vi.fn(),
      deleteRfidTag: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [RfidTagListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: RfidTagStore, useValue: storeMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RfidTagListComponent);
    await fixture.whenStable();
  });

  it('should load RFID tags on init', () => {
    expect(storeMock.loadAllRfidTags).toHaveBeenCalledTimes(1);
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
    storeMock.loadAllRfidTags.mockClear();
    fixture.nativeElement.querySelector('.rfid-tag-list-header button').click();

    expect(storeMock.loadAllRfidTags).toHaveBeenCalledTimes(1);
  });

  describe('delete', () => {
    function clickDeleteOfFirstRow(dialogResult: boolean): void {
      const dialog = fixture.debugElement.injector.get(MatDialog);
      vi.spyOn(dialog, 'open').mockReturnValue({ afterClosed: () => of(dialogResult) } as never);

      fixture.nativeElement.querySelector('tr[mat-row] button').click();
    }

    it('should delete the RFID tag after confirmation', () => {
      clickDeleteOfFirstRow(true);

      expect(storeMock.deleteRfidTag).toHaveBeenCalledWith('tag-1');
    });

    it('should not delete the RFID tag when the dialog is cancelled', () => {
      clickDeleteOfFirstRow(false);

      expect(storeMock.deleteRfidTag).not.toHaveBeenCalled();
    });
  });
});
