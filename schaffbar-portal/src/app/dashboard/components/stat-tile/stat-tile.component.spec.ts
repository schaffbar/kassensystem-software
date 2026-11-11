import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { StatTileComponent } from './stat-tile.component';

describe('StatTileComponent', () => {
  let fixture: ComponentFixture<StatTileComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [StatTileComponent],
      providers: [provideZonelessChangeDetection()],
    });
    fixture = TestBed.createComponent(StatTileComponent);
    fixture.componentRef.setInput('label', 'Werkzeuge in Benutzung');
    fixture.componentRef.setInput('value', 3);
  });

  function text(selector: string): string | null {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim() ?? null;
  }

  it('should show label and value alone', async () => {
    await fixture.whenStable();

    expect(text('.label')).toBe('Werkzeuge in Benutzung');
    expect(text('.value')).toBe('3');
    expect(text('.detail')).toBeNull();
  });

  it('should show the total and the detail', async () => {
    fixture.componentRef.setInput('total', 18);
    fixture.componentRef.setInput('detail', 'Bandsäge, Hobel');
    await fixture.whenStable();

    expect(text('.value')).toBe('3 / 18');
    expect(text('.detail')).toBe('Bandsäge, Hobel');
  });
});
