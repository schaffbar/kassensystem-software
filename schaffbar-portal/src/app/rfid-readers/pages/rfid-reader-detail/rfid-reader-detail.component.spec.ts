import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { RfidReaderDetailInfoComponent } from '../../components/rfid-reader-detail-info/rfid-reader-detail-info.component';
import { RfidReaderDetailTypeComponent } from '../../components/rfid-reader-detail-type/rfid-reader-detail-type.component';
import { UpdateRfidReaderFormComponent } from '../../components/update-rfid-reader-form/update-rfid-reader-form.component';
import { ChangeRfidReaderTypeCommand, RfidReader, UpdateRfidReaderCommand } from '../../rfid-reader.model';
import { RfidReaderDetailComponent } from './rfid-reader-detail.component';
import { RfidReaderDetailStore } from './rfid-reader-detail.store';

describe('RfidReaderDetailComponent', () => {
  const rfidReader: RfidReader = {
    id: 'reader-1',
    macAddress: 'AA:BB',
    type: RfidReaderType.SwitchBox,
    name: 'Reader 1',
    socketName: null,
    createdAt: new Date('2026-01-01T10:00:00Z'),
  };

  let fixture: ComponentFixture<RfidReaderDetailComponent>;
  let storeMock: {
    rfidReader: WritableSignal<RfidReader | null>;
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    setRfidReaderId: Mock;
    reloadRfidReader: Mock;
    canChangeRfidReaderType: Mock;
    updateRfidReader: Mock;
    changeRfidReaderType: Mock;
    deleteRfidReader: Mock;
  };
  let openDialog: Mock;
  let openSnackBar: Mock;
  let openConfirmDelete: Mock;

  beforeEach(async () => {
    storeMock = {
      rfidReader: signal(rfidReader),
      isLoading: signal(false),
      hasLoadError: signal(false),
      setRfidReaderId: vi.fn(),
      reloadRfidReader: vi.fn(),
      canChangeRfidReaderType: vi.fn(),
      updateRfidReader: vi.fn(),
      changeRfidReaderType: vi.fn(),
      deleteRfidReader: vi.fn(),
    };
    openDialog = vi.fn();
    openSnackBar = vi.fn();
    openConfirmDelete = vi.fn();

    await TestBed.configureTestingModule({
      imports: [RfidReaderDetailComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: MatDialog, useValue: { open: openDialog } },
        { provide: MatSnackBar, useValue: { open: openSnackBar } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDelete } },
      ],
    })
      .overrideComponent(RfidReaderDetailComponent, {
        set: { providers: [{ provide: RfidReaderDetailStore, useValue: storeMock }] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(RfidReaderDetailComponent);
    fixture.componentRef.setInput('id', 'reader-1');
    await fixture.whenStable();
  });

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should load the reader of the route', () => {
    expect(storeMock.setRfidReaderId).toHaveBeenCalledOnce();
  });

  it('should show the name as title with type and MAC address and link back to the list', () => {
    expect(query('h1')?.textContent?.trim()).toBe('Reader 1');
    expect(query('schbar-status-badge .label')?.textContent?.trim()).toBe('rfidReaders.types.switch_box');
    expect(query('schbar-status-badge mat-icon')).toBeNull();
    expect(query('.icon-tile mat-icon')?.textContent).toBe('electrical_services');
    expect(query('.mac-address')?.textContent?.trim()).toBe('AA:BB');
    expect(query('.breadcrumbs a')?.getAttribute('href')).toBe('/rfid-readers');
  });

  it('should show the MAC address as title and a warning badge for a new reader', async () => {
    storeMock.rfidReader.set({ ...rfidReader, name: null, type: null });
    await fixture.whenStable();

    expect(query('h1')?.textContent?.trim()).toBe('AA:BB');
    expect(query('schbar-status-badge.tone-danger .label')?.textContent?.trim()).toBe('rfidReaders.types.none');
  });

  it('should pass the reader to its cards', () => {
    expect(child(RfidReaderDetailInfoComponent).rfidReaderDetailInfo()).toBe(rfidReader);
    expect(child(RfidReaderDetailTypeComponent).rfidReaderDetailType()).toBe(rfidReader);
  });

  describe('edit', () => {
    const command: UpdateRfidReaderCommand = { rfidReaderId: 'reader-1', name: 'Neu', socketName: 'neu' };

    it('should open the form from the info card and update the reader', () => {
      openDialog.mockReturnValue({ afterClosed: () => of(command) });

      child(RfidReaderDetailInfoComponent).editRequested.emit();

      expect(openDialog).toHaveBeenCalledExactlyOnceWith(
        UpdateRfidReaderFormComponent,
        expect.objectContaining({ ...FORM_DIALOG_CONFIG, data: { rfidReader } }),
      );
      expect(storeMock.updateRfidReader).toHaveBeenCalledExactlyOnceWith(command);
    });

    it('should open the form from the header', () => {
      openDialog.mockReturnValue({ afterClosed: () => of(command) });

      query('schbar-page-header .actions button[matButton]')?.click();

      expect(storeMock.updateRfidReader).toHaveBeenCalledExactlyOnceWith(command);
    });

    it('should not update the reader when the form is cancelled', () => {
      openDialog.mockReturnValue({ afterClosed: () => of(undefined) });

      child(RfidReaderDetailInfoComponent).editRequested.emit();

      expect(storeMock.updateRfidReader).not.toHaveBeenCalled();
    });
  });

  describe('change type', () => {
    const command: ChangeRfidReaderTypeCommand = { rfidReaderId: 'reader-1', type: RfidReaderType.GateKeeperIn };

    function requestTypeChange(): void {
      child(RfidReaderDetailTypeComponent).typeChangeRequested.emit(command);
    }

    it('should change the type without asking when it may be changed', () => {
      storeMock.canChangeRfidReaderType.mockReturnValue(of(true));

      requestTypeChange();

      expect(storeMock.changeRfidReaderType).toHaveBeenCalledExactlyOnceWith(command);
      expect(openSnackBar).not.toHaveBeenCalled();
    });

    it('should show a hint instead of changing the type when it may not be changed', () => {
      storeMock.canChangeRfidReaderType.mockReturnValue(of(false));

      requestTypeChange();

      expect(openSnackBar).toHaveBeenCalledExactlyOnceWith(
        'rfidReaders.dialogs.changeRfidReaderType.errors.switchBoxHasAssignedTool',
        'shared.actions.close',
        expect.anything(),
      );
      expect(storeMock.changeRfidReaderType).not.toHaveBeenCalled();
    });
  });

  describe('delete', () => {
    it('should delete the reader from the danger zone after confirmation', () => {
      openConfirmDelete.mockReturnValue(of(true));

      query('schbar-danger-zone button')?.click();

      expect(openConfirmDelete).toHaveBeenCalledExactlyOnceWith({
        title: 'rfidReaders.dialogs.deleteRfidReader.title',
        entity: 'Reader 1',
        message: 'rfidReaders.dialogs.deleteRfidReader.message',
        confirmLabel: 'rfidReaders.dialogs.deleteRfidReader.confirm',
      });
      expect(storeMock.deleteRfidReader).toHaveBeenCalledExactlyOnceWith({ rfidReaderId: 'reader-1' });
    });

    it('should delete the reader from the ⋯ menu after confirmation', async () => {
      openConfirmDelete.mockReturnValue(of(true));

      query('schbar-row-menu button')?.click();
      await fixture.whenStable();
      document.querySelector<HTMLElement>('.danger-menu-item')?.click();

      expect(storeMock.deleteRfidReader).toHaveBeenCalledExactlyOnceWith({ rfidReaderId: 'reader-1' });
    });

    it('should not delete the reader when the dialog is cancelled', () => {
      openConfirmDelete.mockReturnValue(of(false));

      query('schbar-danger-zone button')?.click();

      expect(storeMock.deleteRfidReader).not.toHaveBeenCalled();
    });
  });
});
