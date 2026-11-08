import { Clipboard } from '@angular/cdk/clipboard';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { LOCALE_ID, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RfidReaderDetailInfoComponent, RfidReaderDetailInfoViewModel } from './rfid-reader-detail-info.component';

registerLocaleData(localeDe);

describe('RfidReaderDetailInfoComponent', () => {
  const rfidReader: RfidReaderDetailInfoViewModel = {
    name: 'Tor Ausgang',
    macAddress: 'AA:BB:CC:DD:EE:FF',
    socketName: 'gate-out',
    createdAt: new Date('2025-01-14T10:00:00Z'),
  };

  let fixture: ComponentFixture<RfidReaderDetailInfoComponent>;
  let copy: Mock;

  beforeEach(async () => {
    copy = vi.fn().mockReturnValue(true);

    await TestBed.configureTestingModule({
      imports: [RfidReaderDetailInfoComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: LOCALE_ID, useValue: 'de-DE' },
        { provide: Clipboard, useValue: { copy } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RfidReaderDetailInfoComponent);
    fixture.componentRef.setInput('rfidReaderDetailInfo', rfidReader);
    await fixture.whenStable();
  });

  afterEach(() => vi.useRealTimers());

  function text(selector: string): string {
    return fixture.nativeElement.querySelector(selector)?.textContent.trim() ?? '';
  }

  function copyButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.mac-address button');
  }

  it('should show the values of the reader', () => {
    expect(text('.name')).toBe('Tor Ausgang');
    expect(text('.mac-address .mono')).toBe('AA:BB:CC:DD:EE:FF');
    expect(text('.socket-name')).toBe('gate-out');
    expect(text('.created-at')).toBe('14.01.2025');
  });

  it('should show a dash for a reader without name and socket name', async () => {
    fixture.componentRef.setInput('rfidReaderDetailInfo', { ...rfidReader, name: null, socketName: null });
    await fixture.whenStable();

    expect(text('.name')).toBe('–');
    expect(text('.socket-name')).toBe('–');
  });

  it('should request editing', () => {
    const editRequested = vi.fn();
    fixture.componentInstance.editRequested.subscribe(editRequested);

    fixture.nativeElement.querySelector('.card-header button').click();

    expect(editRequested).toHaveBeenCalledOnce();
  });

  // whenStable() would wait for the fake timer, so the changes are detected by hand
  it('should copy the MAC address and confirm it for a while', () => {
    vi.useFakeTimers();

    copyButton().click();
    fixture.detectChanges();

    expect(copy).toHaveBeenCalledExactlyOnceWith('AA:BB:CC:DD:EE:FF');
    expect(text('.mac-address mat-icon')).toBe('check');
    expect(text('[aria-live]')).toBe('rfidReaders.pages.rfidReaderDetails.actions.macAddressCopied');

    vi.advanceTimersByTime(2000);
    fixture.detectChanges();

    expect(text('.mac-address mat-icon')).toBe('content_copy');
    expect(text('[aria-live]')).toBe('');
  });

  it('should not confirm when copying failed', async () => {
    copy.mockReturnValue(false);

    copyButton().click();
    await fixture.whenStable();

    expect(text('.mac-address mat-icon')).toBe('content_copy');
  });
});
