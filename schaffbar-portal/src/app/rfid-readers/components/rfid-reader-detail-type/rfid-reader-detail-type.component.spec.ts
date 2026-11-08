import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatRadioGroupHarness } from '@angular/material/radio/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { RfidReaderDetailTypeComponent, RfidReaderDetailTypeViewModel } from './rfid-reader-detail-type.component';

describe('RfidReaderDetailTypeComponent', () => {
  const rfidReader: RfidReaderDetailTypeViewModel = { id: 'reader-1', type: RfidReaderType.GateKeeperOut };

  let fixture: ComponentFixture<RfidReaderDetailTypeComponent>;
  let loader: HarnessLoader;
  let typeChangeRequested: Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RfidReaderDetailTypeComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(RfidReaderDetailTypeComponent);
    fixture.componentRef.setInput('rfidReaderDetailType', rfidReader);
    typeChangeRequested = vi.fn();
    fixture.componentInstance.typeChangeRequested.subscribe(typeChangeRequested);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  async function click(selector: string): Promise<void> {
    query(selector)?.click();
    await fixture.whenStable();
  }

  async function startEditing(): Promise<void> {
    await click('.card-header button');
  }

  async function select(type: RfidReaderType): Promise<void> {
    const group = await loader.getHarness(MatRadioGroupHarness);
    await group.checkRadioButton({ label: new RegExp(`rfidReaders\\.types\\.${type.toLowerCase()}`) });
  }

  async function save(): Promise<void> {
    query('form')?.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
  }

  it('should show the current type with its description', () => {
    expect(query('.current-type .type-label')?.textContent?.trim()).toBe('rfidReaders.types.gate_keeper_out');
    expect(query('.current-type .type-description')?.textContent?.trim()).toBe(
      'rfidReaders.typeDescriptions.gate_keeper_out',
    );
    expect(query('.current-type.type-missing')).toBeNull();
  });

  it('should highlight a reader without type', async () => {
    fixture.componentRef.setInput('rfidReaderDetailType', { ...rfidReader, type: null });
    await fixture.whenStable();

    expect(query('.current-type.type-missing .type-label')?.textContent?.trim()).toBe('rfidReaders.types.none');
  });

  it('should offer all types with the current one selected', async () => {
    await startEditing();

    const group = await loader.getHarness(MatRadioGroupHarness);
    expect(await group.getRadioButtons()).toHaveLength(Object.values(RfidReaderType).length);
    expect(await group.getCheckedValue()).toBe(RfidReaderType.GateKeeperOut);
    expect(query('.radio-card.selected .radio-card-label')?.textContent?.trim()).toBe(
      'rfidReaders.types.gate_keeper_out',
    );
    expect(query('.current-type')).toBeNull();
  });

  it('should request the change to the selected type', async () => {
    await startEditing();
    await select(RfidReaderType.GateKeeperIn);

    await save();

    expect(typeChangeRequested).toHaveBeenCalledExactlyOnceWith({
      rfidReaderId: 'reader-1',
      type: RfidReaderType.GateKeeperIn,
    });
    // stays open until the reader has its new type – the page may still refuse the change
    expect(query('form')).not.toBeNull();
  });

  it('should close the selection once the reader has its new type', async () => {
    await startEditing();
    await select(RfidReaderType.GateKeeperIn);
    await save();

    fixture.componentRef.setInput('rfidReaderDetailType', { ...rfidReader, type: RfidReaderType.GateKeeperIn });
    await fixture.whenStable();

    expect(query('form')).toBeNull();
    expect(query('.current-type .type-label')?.textContent?.trim()).toBe('rfidReaders.types.gate_keeper_in');
  });

  it('should just close the selection when the type was not changed', async () => {
    await startEditing();

    await save();

    expect(typeChangeRequested).not.toHaveBeenCalled();
    expect(query('form')).toBeNull();
  });

  it('should restore the current type on cancel without requesting a change', async () => {
    await startEditing();
    await select(RfidReaderType.SwitchBox);

    await click('.form-actions button[type="button"]');
    await startEditing();

    const group = await loader.getHarness(MatRadioGroupHarness);
    expect(await group.getCheckedValue()).toBe(RfidReaderType.GateKeeperOut);
    expect(typeChangeRequested).not.toHaveBeenCalled();
  });

  it('should require a type for a reader without type', async () => {
    fixture.componentRef.setInput('rfidReaderDetailType', { ...rfidReader, type: null });
    await fixture.whenStable();
    await startEditing();

    await save();

    expect(typeChangeRequested).not.toHaveBeenCalled();
    expect(query('.type-error')?.textContent?.trim()).toBe('shared.errors.required');
  });
});
