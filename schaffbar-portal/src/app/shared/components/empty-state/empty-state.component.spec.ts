import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { EmptyStateComponent } from './empty-state.component';

describe('EmptyStateComponent', () => {
  let fixture: ComponentFixture<EmptyStateComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [EmptyStateComponent],
      providers: [provideZonelessChangeDetection()],
    });
    fixture = TestBed.createComponent(EmptyStateComponent);
    fixture.componentRef.setInput('icon', 'build');
    fixture.componentRef.setInput('title', 'Noch keine Werkzeuge');
    await fixture.whenStable();
  });

  function text(selector: string): string | undefined {
    return fixture.nativeElement.querySelector(selector)?.textContent?.trim();
  }

  it('should show icon and title, no message by default', () => {
    expect(text('mat-icon')).toBe('build');
    expect(text('.title')).toBe('Noch keine Werkzeuge');
    expect(fixture.nativeElement.querySelector('.message')).toBeNull();
  });

  it('should show the message', async () => {
    fixture.componentRef.setInput('message', 'Lege das erste Werkzeug an.');
    await fixture.whenStable();

    expect(text('.message')).toBe('Lege das erste Werkzeug an.');
  });
});
