import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { SegmentedControlComponent, SegmentViewModel } from './segmented-control.component';

@Component({
  imports: [SegmentedControlComponent],
  template: `<schbar-segmented-control label="Ansicht" [segments]="segments" [(selected)]="selected" />`,
})
class HostComponent {
  readonly segments: SegmentViewModel[] = [
    { id: 'cards', label: 'Karten', icon: 'grid_view' },
    { id: 'HOLZ', label: 'Holz', count: 5 },
  ];
  readonly selected = signal('cards');
}

describe('SegmentedControlComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideZonelessChangeDetection()],
    });
    fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
  });

  function segments(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('button'));
  }

  it('should name the group and show icon, label and count of the segments', () => {
    expect(fixture.nativeElement.querySelector('schbar-segmented-control').getAttribute('aria-label')).toBe('Ansicht');
    expect(segments().map((segment) => segment.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
      'grid_view Karten',
      'Holz 5',
    ]);
  });

  it('should mark the selected segment and select another one on click', async () => {
    expect(segments().map((segment) => segment.getAttribute('aria-pressed'))).toEqual(['true', 'false']);

    segments()[1].click();
    await fixture.whenStable();

    expect(fixture.componentInstance.selected()).toBe('HOLZ');
    expect(segments().map((segment) => segment.getAttribute('aria-pressed'))).toEqual(['false', 'true']);
  });
});
