import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { NotFoundComponent } from './not-found.component';

describe('NotFoundComponent', () => {
  let fixture: ComponentFixture<NotFoundComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotFoundComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(NotFoundComponent);
    await fixture.whenStable();
  });

  it('should explain the missing page and link to the dashboard', () => {
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('notFound.title');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/dashboard');
  });
});
