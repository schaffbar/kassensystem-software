import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { ToggleChipComponent } from './toggle-chip.component';

@Component({
  imports: [ToggleChipComponent],
  template: `<schbar-toggle-chip label="Hardware einrichten" icon="warning" [count]="7" [(pressed)]="pressed" />`,
})
class HostComponent {
  readonly pressed = signal(false);
}

describe('ToggleChipComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideZonelessChangeDetection()],
    });
    fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
  });

  function chip(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button');
  }

  it('should show icon, label and count', () => {
    expect(chip().textContent?.replace(/\s+/g, ' ').trim()).toBe('warning Hardware einrichten 7');
    expect(chip().getAttribute('aria-pressed')).toBe('false');
  });

  it('should switch on and off on click', async () => {
    chip().click();
    await fixture.whenStable();
    expect(fixture.componentInstance.pressed()).toBe(true);
    expect(chip().getAttribute('aria-pressed')).toBe('true');
    expect(chip().classList).toContain('selected');

    chip().click();
    await fixture.whenStable();
    expect(fixture.componentInstance.pressed()).toBe(false);
  });
});
