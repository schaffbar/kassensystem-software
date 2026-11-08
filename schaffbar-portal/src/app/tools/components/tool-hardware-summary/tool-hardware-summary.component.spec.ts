import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { ToolHardwareSummaryComponent, ToolHardwareSummaryViewModel } from './tool-hardware-summary.component';

describe('ToolHardwareSummaryComponent', () => {
  const hardware: ToolHardwareSummaryViewModel = {
    rfidReader: { id: 'reader-1', name: 'Schaltbox Lasercutter', macAddress: 'AA:BB' },
    wlanRelaisType: WlanRelaisType.ShellyPro,
    ipAddress: '192.168.10.42',
  };

  let fixture: ComponentFixture<ToolHardwareSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolHardwareSummaryComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolHardwareSummaryComponent);
    fixture.componentRef.setInput('toolHardwareSummary', hardware);
    await fixture.whenStable();
  });

  function text(selector: string): string {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim() ?? '';
  }

  it('should link the switch box and show the relais with its IP address', () => {
    expect(text('.rfid-reader .part-value')).toBe('Schaltbox Lasercutter');
    expect(fixture.nativeElement.querySelector('.rfid-reader a').getAttribute('href')).toBe('/rfid-readers/reader-1');
    expect(text('.wlan-relais .part-value')).toBe('tools.wlanRelaisTypes.SHELLY_PRO · 192.168.10.42');
    expect(fixture.nativeElement.querySelectorAll('.missing')).toHaveLength(0);
  });

  it('should name a reader without name by its MAC address', async () => {
    fixture.componentRef.setInput('toolHardwareSummary', {
      ...hardware,
      rfidReader: { ...hardware.rfidReader, name: null },
    });
    await fixture.whenStable();

    expect(text('.rfid-reader .part-value')).toBe('tools.pages.toolDetails.hardware.unnamedRfidReader');
  });

  it('should mark what is missing', async () => {
    fixture.componentRef.setInput('toolHardwareSummary', { rfidReader: null, wlanRelaisType: null, ipAddress: null });
    await fixture.whenStable();

    expect(text('.rfid-reader.missing .part-value')).toBe('tools.pages.toolDetails.hardware.noRfidReader');
    expect(text('.wlan-relais.missing .part-value')).toBe('tools.pages.toolDetails.hardware.noWlanRelais');
  });

  it('should request the hardware configuration', () => {
    const configureRequested = vi.fn();
    fixture.componentInstance.configureRequested.subscribe(configureRequested);

    fixture.nativeElement.querySelector('.configure-button').click();

    expect(configureRequested).toHaveBeenCalledOnce();
  });
});
