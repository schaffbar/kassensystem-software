import { Component, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { DangerZoneComponent } from './danger-zone.component';

@Component({
  imports: [DangerZoneComponent],
  template: `
    <schbar-danger-zone message="Entfernt das Gerät aus dem System.">
      <button type="button">Lesegerät löschen …</button>
    </schbar-danger-zone>
  `,
})
class HostComponent {}

describe('DangerZoneComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    });
    fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
  });

  function text(selector: string): string | undefined {
    return fixture.nativeElement.querySelector(selector)?.textContent.trim();
  }

  it('should show the title, the message and the projected action', () => {
    expect(text('h2')).toBe('shared.dangerZone.title');
    expect(text('p')).toBe('Entfernt das Gerät aus dem System.');
    expect(text('.action button')).toBe('Lesegerät löschen …');
  });
});
