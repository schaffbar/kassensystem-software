import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { RfidReaderListItem } from '../../rfid-reader.model';
import { RfidReadersStore } from '../../rfid-readers.store';
import { RfidReaderListComponent } from './rfid-reader-list.component';

describe('RfidReaderListComponent', () => {
  const reader = (overrides: Partial<RfidReaderListItem>): RfidReaderListItem => ({
    id: 'reader',
    macAddress: 'AA:BB',
    type: RfidReaderType.SwitchBox,
    name: null,
    socketName: null,
    createdAt: new Date('2026-01-01T10:00:00Z'),
    assignedTool: null,
    ...overrides,
  });
  const rfidReaders: RfidReaderListItem[] = [
    reader({
      id: 'box-saw',
      name: 'Schaltbox Bandsäge',
      macAddress: '3C:01',
      socketName: 'box-bandsaege',
      assignedTool: { id: 'tool-1', name: 'Bandsäge' },
    }),
    reader({ id: 'box-07', name: 'Schaltbox 07', macAddress: '3C:02' }),
    reader({ id: 'gate-out', name: 'Tor Ausgang', macAddress: '3C:03', type: RfidReaderType.GateKeeperOut }),
    reader({ id: 'desk', name: 'Theke Register', macAddress: '3C:04', type: RfidReaderType.RfidTagRegister }),
    reader({ id: 'new', macAddress: '3C:05', type: null }),
  ];

  let fixture: ComponentFixture<RfidReaderListComponent>;
  let storeMock: {
    rfidReaders: WritableSignal<RfidReaderListItem[]>;
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    reloadRfidReaders: Mock;
    deleteRfidReader: Mock;
  };
  let openConfirmDeleteDialog: Mock;

  beforeEach(async () => {
    storeMock = {
      rfidReaders: signal(rfidReaders),
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

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  }

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  async function search(text: string): Promise<void> {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = text;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function selectFilter(index: number): Promise<void> {
    fixture.nativeElement.querySelectorAll('schbar-filter-chips button')[index].click();
    await fixture.whenStable();
  }

  async function clickRowMenuItem(row: number, item: 'details' | 'delete'): Promise<void> {
    fixture.nativeElement.querySelectorAll('tr[mat-row] schbar-row-menu button')[row].click();
    await fixture.whenStable();
    const items = document.querySelectorAll<HTMLElement>('[mat-menu-item]');
    items[item === 'details' ? 0 : 1].click();
  }

  it('should load the RFID readers on init', () => {
    expect(storeMock.reloadRfidReaders).toHaveBeenCalledOnce();
  });

  describe('groups', () => {
    it('should show new readers, gates and counter as cards, sorted by name', () => {
      expect(texts('.new-readers .card-name')).toEqual(['3C:05']);
      expect(texts('.new-readers schbar-status-badge')).toEqual(['warning rfidReaders.types.none']);
      expect(texts('.desk-readers .card-name')).toEqual(['Theke Register', 'Tor Ausgang']);
      expect(texts('.desk-readers .card-type')).toEqual([
        'rfidReaders.types.rfid_tag_register',
        'rfidReaders.types.gate_keeper_out',
      ]);
    });

    it('should link a card to the detail page of its reader', () => {
      expect(query('.desk-readers a.reader-card')?.getAttribute('href')).toBe('/desk');
    });

    it('should show the switch boxes in a table with their tool', () => {
      expect(texts('.switch-boxes td.name')).toEqual(['Schaltbox 07', 'Schaltbox Bandsäge']);
      expect(texts('.switch-boxes td.mat-column-tool')).toEqual([
        'link_off rfidReaders.pages.rfidReaderList.unassigned',
        'Bandsäge',
      ]);
      expect(query('.tool-link')?.getAttribute('href')).toBe('/tools/tool-1');
      expect(texts('.switch-boxes td.socket-name')).toEqual(['–', 'box-bandsaege']);
    });

    it('should leave out a group without readers', async () => {
      storeMock.rfidReaders.set([rfidReaders[0]]);
      await fixture.whenStable();

      expect(query('.new-readers')).toBeNull();
      expect(query('.desk-readers')).toBeNull();
      expect(query('.switch-boxes')).not.toBeNull();
    });
  });

  describe('filter and search', () => {
    it('should count the readers of every filter', () => {
      expect(texts('schbar-filter-chips button')).toEqual([
        'rfidReaders.pages.rfidReaderList.filters.all 5',
        'rfidReaders.pages.rfidReaderList.filters.desk 2',
        'rfidReaders.pages.rfidReaderList.filters.switchBox 2',
        'rfidReaders.pages.rfidReaderList.filters.unassigned 2',
      ]);
    });

    it('should show only the gates and the counter', async () => {
      await selectFilter(1);

      expect(query('.new-readers')).toBeNull();
      expect(query('.switch-boxes')).toBeNull();
      expect(texts('.desk-readers .card-name')).toEqual(['Theke Register', 'Tor Ausgang']);
    });

    it('should show the readers that are not set up yet: without type, switch boxes without tool', async () => {
      await selectFilter(3);

      expect(query('.desk-readers')).toBeNull();
      expect(texts('.new-readers .card-name')).toEqual(['3C:05']);
      expect(texts('.switch-boxes td.name')).toEqual(['Schaltbox 07']);
    });

    it('should search in name, MAC address, socket name and tool, keeping the counts', async () => {
      await search('bandsäge');
      expect(texts('td.name')).toEqual(['Schaltbox Bandsäge']);
      expect(query('.desk-readers')).toBeNull();

      await search('3c:04');
      expect(texts('.card-name')).toEqual(['Theke Register']);

      await search('box-bandsaege');
      expect(texts('td.name')).toEqual(['Schaltbox Bandsäge']);

      expect(texts('schbar-filter-chips button')[0]).toBe('rfidReaders.pages.rfidReaderList.filters.all 5');
    });

    it('should say so when nothing matches and clear the search', async () => {
      await search('lasercutter');

      expect(texts('schbar-empty-state.no-results .title')).toEqual([
        'rfidReaders.pages.rfidReaderList.noResults.title',
      ]);

      query('.search-field button')?.click();
      await fixture.whenStable();

      expect(query('schbar-empty-state.no-results')).toBeNull();
      expect(texts('td.name')).toHaveLength(2);
    });
  });

  describe('empty list', () => {
    it('should show the empty state instead of toolbar and groups without readers', async () => {
      storeMock.rfidReaders.set([]);
      await fixture.whenStable();

      expect(texts('schbar-empty-state .title')).toEqual(['rfidReaders.pages.rfidReaderList.empty.title']);
      expect(query('.toolbar')).toBeNull();
    });

    it('should not show the empty state while loading or after a load error', async () => {
      storeMock.rfidReaders.set([]);
      storeMock.isLoading.set(true);
      await fixture.whenStable();
      expect(query('schbar-empty-state')).toBeNull();

      storeMock.isLoading.set(false);
      storeMock.hasLoadError.set(true);
      await fixture.whenStable();
      expect(query('schbar-empty-state')).toBeNull();
    });
  });

  describe('switch box rows', () => {
    it('should open the detail page of a clicked row', () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

      query('tr[mat-row]')?.click();

      expect(navigate).toHaveBeenCalledExactlyOnceWith(['rfid-readers', 'box-07']);
    });

    it('should open the detail page from the row menu', async () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

      await clickRowMenuItem(1, 'details');

      expect(navigate).toHaveBeenCalledExactlyOnceWith(['rfid-readers', 'box-saw']);
    });

    it('should delete the reader after confirmation without opening its detail page', async () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      openConfirmDeleteDialog.mockReturnValue(of(true));

      await clickRowMenuItem(0, 'delete');

      expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'rfidReaders.dialogs.deleteRfidReader.title',
        entity: 'Schaltbox 07',
        message: 'rfidReaders.dialogs.deleteRfidReader.message',
        confirmLabel: 'rfidReaders.dialogs.deleteRfidReader.confirm',
      });
      expect(storeMock.deleteRfidReader).toHaveBeenCalledExactlyOnceWith({ rfidReaderId: 'box-07' });
      expect(navigate).not.toHaveBeenCalled();
    });

    it('should not delete the reader when the dialog is cancelled', async () => {
      openConfirmDeleteDialog.mockReturnValue(of(false));

      await clickRowMenuItem(0, 'delete');

      expect(storeMock.deleteRfidReader).not.toHaveBeenCalled();
    });

    it('should not offer to delete a switch box with a tool', async () => {
      fixture.nativeElement.querySelectorAll('tr[mat-row] schbar-row-menu button')[1].click();
      await fixture.whenStable();

      expect(document.querySelectorAll('[mat-menu-item]').length).toBe(1);
      expect(document.querySelector('.danger-menu-item')).toBeNull();
    });
  });
});
