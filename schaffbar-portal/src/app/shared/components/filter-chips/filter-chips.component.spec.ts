import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { FilterChipsComponent, FilterChipViewModel } from './filter-chips.component';

@Component({
  imports: [FilterChipsComponent],
  template: `<schbar-filter-chips label="Filter" [filterChips]="filterChips" [(selected)]="selected" />`,
})
class HostComponent {
  readonly filterChips: FilterChipViewModel[] = [
    { id: 'all', label: 'Alle', count: 14 },
    { id: 'switchBox', label: 'Schaltboxen', count: 10 },
  ];
  readonly selected = signal('all');
}

describe('FilterChipsComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideZonelessChangeDetection()],
    });
    fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
  });

  function chips(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('button'));
  }

  it('should name the group and show every filter with its count', () => {
    expect(fixture.nativeElement.querySelector('schbar-filter-chips').getAttribute('aria-label')).toBe('Filter');
    expect(chips().map((chip) => chip.textContent?.replace(/\s+/g, ' ').trim())).toEqual(['Alle 14', 'Schaltboxen 10']);
  });

  it('should mark the selected filter as pressed', () => {
    expect(chips().map((chip) => chip.getAttribute('aria-pressed'))).toEqual(['true', 'false']);
    expect(chips()[0].classList).toContain('selected');
  });

  it('should select a filter on click', async () => {
    chips()[1].click();
    await fixture.whenStable();

    expect(fixture.componentInstance.selected()).toBe('switchBox');
    expect(chips().map((chip) => chip.getAttribute('aria-pressed'))).toEqual(['false', 'true']);
  });
});
