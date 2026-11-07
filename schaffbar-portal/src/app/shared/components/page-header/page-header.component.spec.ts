import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { BreadcrumbViewModel, PageHeaderComponent } from './page-header.component';

@Component({
  imports: [PageHeaderComponent],
  template: `
    <schbar-page-header [title]="title()" [subtitle]="subtitle()" [breadcrumbs]="breadcrumbs()">
      <span pageHeaderMeta>Aktiv</span>
      <button type="button">Bearbeiten</button>
    </schbar-page-header>
  `,
})
class HostComponent {
  readonly title = signal('Bandsäge');
  readonly subtitle = signal<string | null>(null);
  readonly breadcrumbs = signal<BreadcrumbViewModel[]>([]);
}

describe('PageHeaderComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideTranslateService()],
    });
    fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
  });

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  it('should show the title and project actions and meta', () => {
    expect(query('h1')?.textContent).toBe('Bandsäge');
    expect(query('.actions button')?.textContent).toBe('Bearbeiten');
    expect(query('.meta [pageHeaderMeta]')?.textContent).toBe('Aktiv');
  });

  it('should show neither subtitle nor breadcrumbs by default', () => {
    expect(query('.subtitle')).toBeNull();
    expect(query('nav')).toBeNull();
  });

  it('should show the subtitle', async () => {
    fixture.componentInstance.subtitle.set('Holz');
    await fixture.whenStable();

    expect(query('.subtitle')?.textContent).toBe('Holz');
  });

  it('should link the breadcrumbs and end with the title as current page', async () => {
    fixture.componentInstance.breadcrumbs.set([{ label: 'Werkzeuge', link: '/tools' }]);
    await fixture.whenStable();

    const link = query('nav a');
    expect(link?.textContent).toBe('Werkzeuge');
    expect(link?.getAttribute('href')).toBe('/tools');
    expect(query('nav [aria-current="page"]')?.textContent).toBe('Bandsäge');
  });
});
