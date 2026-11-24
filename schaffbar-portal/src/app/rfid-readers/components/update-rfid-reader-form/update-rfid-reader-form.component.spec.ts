import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import {
  ExistingRfidReaderViewModel,
  UpdateRfidReaderDialogData,
  UpdateRfidReaderFormComponent,
  UpdateRfidReaderViewModel,
} from './update-rfid-reader-form.component';

describe('UpdateRfidReaderFormComponent', () => {
  const rfidReader: UpdateRfidReaderViewModel = {
    id: 'reader-1',
    name: 'Tor Ausgang',
    socketName: 'gate-out',
    macAddress: 'AA:BB:CC:DD:EE:FF',
    type: RfidReaderType.GateKeeperOut,
  };

  let fixture: ComponentFixture<UpdateRfidReaderFormComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: Mock };
  let existingRfidReaders: WritableSignal<ExistingRfidReaderViewModel[]>;

  async function setUp(data: UpdateRfidReaderViewModel): Promise<void> {
    dialogRef = { close: vi.fn() };
    existingRfidReaders = signal([
      { id: 'reader-1', name: data.name },
      { id: 'reader-2', name: 'Theke' },
      { id: 'reader-3', name: null },
    ]);
    const dialogData: UpdateRfidReaderDialogData = { rfidReader: data, existingRfidReaders };

    await TestBed.configureTestingModule({
      imports: [UpdateRfidReaderFormComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UpdateRfidReaderFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  }

  function input(id: string): Promise<MatInputHarness> {
    return loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
  }

  async function setValue(id: string, value: string): Promise<void> {
    await (await input(id)).setValue(value);
    await fixture.whenStable();
  }

  async function leave(id: string): Promise<void> {
    await (await input(id)).blur();
    await fixture.whenStable();
  }

  async function save(): Promise<void> {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
  }

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  async function errors(id: string): Promise<string[]> {
    return (await loader.getHarness(MatFormFieldHarness.with({ selector: `:has(#${id})` }))).getTextErrors();
  }

  describe('reader with name and socket name', () => {
    beforeEach(() => setUp(rfidReader));

    it('should name the reader in the header and save as "save changes"', () => {
      expect(query('schbar-form-dialog-header .subtitle')?.textContent?.trim()).toBe(
        'Tor Ausgang · rfidReaders.types.gate_keeper_out',
      );
      expect(query('button[type="submit"]')?.textContent?.trim()).toBe('shared.actions.saveChanges');
    });

    it('should name the fields with labels above them', () => {
      expect(query('label[for="name"]')?.textContent?.trim()).toBe('rfidReaders.shared.rfidReaderProperties.name');
      expect(query('label[for="socketName"]')?.textContent?.trim()).toBe(
        'rfidReaders.shared.rfidReaderProperties.socketName',
      );
    });

    it('should fill the form with the current values and show the fixed ones', async () => {
      expect(await (await input('name')).getValue()).toBe('Tor Ausgang');
      expect(await (await input('socketName')).getValue()).toBe('gate-out');
      expect(query('.fixed-values')?.textContent).toContain('AA:BB:CC:DD:EE:FF');
      expect(query('.fixed-values')?.textContent).toContain('rfidReaders.types.gate_keeper_out');
    });

    it('should close with the update command', async () => {
      await setValue('name', 'Tor Eingang');
      await setValue('socketName', 'gate-in');

      await save();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        rfidReaderId: 'reader-1',
        name: 'Tor Eingang',
        socketName: 'gate-in',
      });
    });

    it('should accept an empty name', async () => {
      await setValue('name', '');

      await save();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        rfidReaderId: 'reader-1',
        name: undefined,
        socketName: 'gate-out',
      });
    });

    it('should reject a socket name with other characters than a–z, 0–9 and "-"', async () => {
      await setValue('socketName', 'Gate Out');

      await save();

      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(await errors('socketName')).toEqual(['rfidReaders.dialogs.updateRfidReader.errors.socketNameFormat']);
    });

    it('should keep an unchanged socket name in an older format', async () => {
      TestBed.resetTestingModule();
      await setUp({ ...rfidReader, socketName: 'Socket 43' });
      await setValue('name', 'Kreissäge');

      await save();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        rfidReaderId: 'reader-1',
        name: 'Kreissäge',
        socketName: 'Socket 43',
      });
    });

    it('should reject the name of another reader, also in another case', async () => {
      await setValue('name', 'THEKE');

      await save();

      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(await errors('name')).toEqual(['rfidReaders.dialogs.updateRfidReader.errors.nameExists']);
    });

    it('should show the error at the name as soon as the field is left', async () => {
      await setValue('name', 'Theke');
      expect(await errors('name')).toEqual([]);

      await leave('name');

      expect(await errors('name')).toEqual(['rfidReaders.dialogs.updateRfidReader.errors.nameExists']);
    });

    it('should keep its own name in another case', async () => {
      await setValue('name', 'TOR AUSGANG');

      await save();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        rfidReaderId: 'reader-1',
        name: 'TOR AUSGANG',
        socketName: 'gate-out',
      });
    });

    it('should save a name that differs only by a space at the end, untrimmed', async () => {
      await setValue('name', 'Theke ');

      await save();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        rfidReaderId: 'reader-1',
        name: 'Theke ',
        socketName: 'gate-out',
      });
    });

    it('should follow the list of readers while the dialog is open', async () => {
      await setValue('name', 'Werkstatt Eingang');
      await leave('name');
      expect(await errors('name')).toEqual([]);

      existingRfidReaders.set([...existingRfidReaders(), { id: 'reader-4', name: 'Werkstatt Eingang' }]);
      await fixture.whenStable();

      expect(await errors('name')).toEqual(['rfidReaders.dialogs.updateRfidReader.errors.nameExists']);
    });

    it('should warn only when the socket name was changed to a valid one', async () => {
      expect(query('.socket-name-changed')).toBeNull();

      await setValue('socketName', 'gate-out-2');
      expect(query('.socket-name-changed strong')?.textContent).toBe('gate-out-2');

      await setValue('socketName', 'Gate');
      expect(query('.socket-name-changed')).toBeNull();
    });
  });

  describe('new reader without name and socket name', () => {
    beforeEach(() => setUp({ ...rfidReader, name: null, socketName: null, type: null }));

    it('should start with empty fields and name the reader by its MAC address', async () => {
      expect(await (await input('name')).getValue()).toBe('');
      expect(await (await input('socketName')).getValue()).toBe('');
      expect(query('schbar-form-dialog-header .subtitle')?.textContent?.trim()).toBe(
        'AA:BB:CC:DD:EE:FF · rfidReaders.types.none',
      );
    });

    it('should accept an empty name next to other readers without one', async () => {
      await save();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        rfidReaderId: 'reader-1',
        name: undefined,
        socketName: undefined,
      });
    });

    it('should accept an empty socket name and leave it unchanged', async () => {
      await setValue('name', 'Werkstatt Eingang');

      await save();

      expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith({
        rfidReaderId: 'reader-1',
        name: 'Werkstatt Eingang',
        socketName: undefined,
      });
    });
  });
});
