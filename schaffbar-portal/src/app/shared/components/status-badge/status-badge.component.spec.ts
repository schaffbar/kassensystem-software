import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { StatusBadgeComponent, StatusBadgeTone } from './status-badge.component';

@Component({
  imports: [StatusBadgeComponent],
  template: `<schbar-status-badge [tone]="tone()" [icon]="icon()">Pausiert</schbar-status-badge>`,
})
class HostComponent {
  readonly tone = signal<StatusBadgeTone>('warn');
  readonly icon = signal<string | null>('pause_circle');
}

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideZonelessChangeDetection()],
    });
    fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
  });

  function badge(): HTMLElement {
    return fixture.nativeElement.querySelector('schbar-status-badge');
  }

  it('should show the icon and the projected label', () => {
    expect(badge().querySelector('mat-icon')?.textContent).toBe('pause_circle');
    expect(badge().querySelector('mat-icon')?.getAttribute('aria-hidden')).toBe('true');
    expect(badge().querySelector('.label')?.textContent).toBe('Pausiert');
  });

  it('should show only the label without icon', async () => {
    fixture.componentInstance.icon.set(null);
    await fixture.whenStable();

    expect(badge().querySelector('mat-icon')).toBeNull();
    expect(badge().querySelector('.label')?.textContent).toBe('Pausiert');
  });

  it('should set the tone as class', async () => {
    expect(badge().classList).toContain('tone-warn');

    fixture.componentInstance.tone.set('danger');
    await fixture.whenStable();

    expect(badge().classList).toContain('tone-danger');
    expect(badge().classList).not.toContain('tone-warn');
  });
});
