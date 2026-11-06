import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { ChangeRfidReaderTypeFormComponent } from '../../components/change-rfid-reader-type-form/change-rfid-reader-type-form.component';
import { RfidReaderDetailInfoComponent } from '../../components/rfid-reader-detail-info/rfid-reader-detail-info.component';
import { ChangeRfidReaderTypeCommand, RfidReader } from '../../rfid-reader.model';
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
  };
  let openDialog: Mock;
  let openSnackBar: Mock;

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
    };
    openDialog = vi.fn();
    openSnackBar = vi.fn();

    await TestBed.configureTestingModule({
      imports: [RfidReaderDetailComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MatDialog, useValue: { open: openDialog } },
        { provide: MatSnackBar, useValue: { open: openSnackBar } },
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

  function clickChangeType(): void {
    fixture.nativeElement.querySelector('.rfid-reader-detail-header-title-actions button').click();
  }

  it('should load the reader of the route', () => {
    expect(storeMock.setRfidReaderId).toHaveBeenCalledOnce();
  });

  it('should pass the reader to the info and execute its update command', () => {
    const info: RfidReaderDetailInfoComponent = fixture.debugElement.query(
      By.directive(RfidReaderDetailInfoComponent),
    ).componentInstance;
    expect(info.rfidReaderDetailInfo()).toBe(rfidReader);

    info.rfidReaderChanged.emit({ rfidReaderId: 'reader-1', name: 'Neu' });

    expect(storeMock.updateRfidReader).toHaveBeenCalledExactlyOnceWith({ rfidReaderId: 'reader-1', name: 'Neu' });
  });

  describe('change type', () => {
    const command: ChangeRfidReaderTypeCommand = { rfidReaderId: 'reader-1', type: RfidReaderType.GateKeeperIn };

    it('should open the form and change the type when it may be changed', () => {
      storeMock.canChangeRfidReaderType.mockReturnValue(of(true));
      openDialog.mockReturnValue({ afterClosed: () => of(command) });

      clickChangeType();

      expect(openDialog).toHaveBeenCalledExactlyOnceWith(
        ChangeRfidReaderTypeFormComponent,
        expect.objectContaining({ data: { rfidReader }, disableClose: true }),
      );
      expect(storeMock.changeRfidReaderType).toHaveBeenCalledExactlyOnceWith(command);
      expect(openSnackBar).not.toHaveBeenCalled();
    });

    it('should not change the type when the form is cancelled', () => {
      storeMock.canChangeRfidReaderType.mockReturnValue(of(true));
      openDialog.mockReturnValue({ afterClosed: () => of(undefined) });

      clickChangeType();

      expect(storeMock.changeRfidReaderType).not.toHaveBeenCalled();
    });

    it('should show a hint instead of the form when it may not be changed', () => {
      storeMock.canChangeRfidReaderType.mockReturnValue(of(false));

      clickChangeType();

      expect(openSnackBar).toHaveBeenCalledExactlyOnceWith(
        'rfidReaders.dialogs.changeRfidReaderType.errors.switchBoxHasAssignedTool',
        'shared.actions.close',
        expect.anything(),
      );
      expect(openDialog).not.toHaveBeenCalled();
    });
  });
});
