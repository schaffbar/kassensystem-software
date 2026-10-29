import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidReader, RfidReaderType } from '../../rfid-reader.model';
import { RfidReaderDetailInfoComponent } from './rfid-reader-detail-info.component';

describe('RfidReaderDetailInfoComponent', () => {
  const rfidReader: RfidReader = {
    id: 'reader-1',
    macAddress: 'AA:BB:CC:DD:EE:FF',
    type: RfidReaderType.SwitchBox,
    name: 'Reader 1',
    socketName: 'Steckdose 1',
  };

  let fixture: ComponentFixture<RfidReaderDetailInfoComponent>;
  let loader: HarnessLoader;
  let readerChanged: Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RfidReaderDetailInfoComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(RfidReaderDetailInfoComponent);
    fixture.componentRef.setInput('rfidReader', rfidReader);
    readerChanged = vi.fn();
    fixture.componentInstance.readerChanged.subscribe(readerChanged);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function input(id: string): Promise<MatInputHarness> {
    return loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
  }

  async function click(selector: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ selector }));
    await button.click();
    await fixture.whenStable();
  }

  const editButton = '[aria-label="Edit reader"]';
  const cancelButton = '.form-actions button[type="button"]';
  const saveButton = '.form-actions button[type="submit"]';

  it('should show the reader values read-only', async () => {
    expect(await (await input('macAddress')).getValue()).toBe('AA:BB:CC:DD:EE:FF');
    expect(await (await input('name')).getValue()).toBe('Reader 1');
    expect(await (await input('name')).isReadonly()).toBe(true);
    expect(await (await input('socketName')).isReadonly()).toBe(true);
    expect(await loader.getHarnessOrNull(MatButtonHarness.with({ selector: saveButton }))).toBeNull();
  });

  it('should show the translation key of the reader type', async () => {
    expect(await (await input('type')).getValue()).toBe('rfidReaders.types.switch_box');
  });

  it('should show "none" for a reader without a type (newly registered)', async () => {
    fixture.componentRef.setInput('rfidReader', { ...rfidReader, type: null });
    await fixture.whenStable();

    expect(await (await input('type')).getValue()).toBe('rfidReaders.types.none');
    // the hint must be projected into the subscript below the field, not into the field itself
    const hint: HTMLElement | null = fixture.nativeElement.querySelector(
      '.mat-mdc-form-field-subscript-wrapper mat-hint',
    );
    expect(hint?.textContent).toContain('rfidReaders.pages.rfidReaderDetails.hints.typeMissing');
  });

  it('should show no hint for a reader with a type', () => {
    expect(fixture.nativeElement.querySelector('mat-hint')).toBeNull();
  });

  it('should make name and socket name editable after clicking edit', async () => {
    await click(editButton);

    expect(await (await input('name')).isReadonly()).toBe(false);
    expect(await (await input('socketName')).isReadonly()).toBe(false);
    expect(await (await input('macAddress')).isReadonly()).toBe(true);
  });

  it('should focus the name field after clicking edit', async () => {
    await click(editButton);

    expect(await (await input('name')).isFocused()).toBe(true);
  });

  it('should save exactly once when the form is submitted (e.g. Enter in a field)', async () => {
    await click(editButton);
    await (await input('name')).setValue('Neuer Name');

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(readerChanged).toHaveBeenCalledTimes(1);
    expect(readerChanged).toHaveBeenCalledWith({ id: 'reader-1', name: 'Neuer Name', socketName: 'Steckdose 1' });
  });

  it('should emit the changes on save and switch back to read-only', async () => {
    await click(editButton);
    await (await input('name')).setValue('Neuer Name');
    await (await input('socketName')).setValue('');

    await click(saveButton);

    expect(readerChanged).toHaveBeenCalledWith({ id: 'reader-1', name: 'Neuer Name', socketName: undefined });
    expect(await (await input('name')).isReadonly()).toBe(true);
  });

  it('should restore the values on cancel without emitting', async () => {
    await click(editButton);
    await (await input('name')).setValue('Neuer Name');

    await click(cancelButton);

    expect(readerChanged).not.toHaveBeenCalled();
    expect(await (await input('name')).getValue()).toBe('Reader 1');
    expect(await (await input('name')).isReadonly()).toBe(true);
  });

  it('should take over the values when the rfidReader input changes', async () => {
    fixture.componentRef.setInput('rfidReader', { ...rfidReader, name: 'Reader 2' });
    await fixture.whenStable();

    expect(await (await input('name')).getValue()).toBe('Reader 2');
  });
});
