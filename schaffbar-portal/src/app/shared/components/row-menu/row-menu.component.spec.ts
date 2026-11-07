import { Component, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatMenuModule } from '@angular/material/menu';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { RowMenuComponent } from './row-menu.component';

@Component({
  imports: [RowMenuComponent, MatMenuModule],
  template: `
    <div class="row">
      <schbar-row-menu label="Aktionen für Bandsäge">
        <button type="button" mat-menu-item (click)="deleted()">Werkzeug löschen</button>
      </schbar-row-menu>
    </div>
  `,
})
class HostComponent {
  readonly deleted = vi.fn();
}

describe('RowMenuComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  const rowClicked = vi.fn();

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideZonelessChangeDetection()],
    });
    fixture = TestBed.createComponent(HostComponent);
    rowClicked.mockClear();
    fixture.nativeElement.querySelector('.row').addEventListener('click', rowClicked);
    await fixture.whenStable();
  });

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  function trigger(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('schbar-row-menu button');
  }

  async function openMenu(): Promise<void> {
    trigger().click();
    await fixture.whenStable();
  }

  it('should name the ⋯ button with the label', () => {
    expect(trigger().getAttribute('aria-label')).toBe('Aktionen für Bandsäge');
  });

  it('should open the projected entries without clicking the row', async () => {
    await openMenu();

    const items = Array.from<HTMLElement>(document.querySelectorAll('[mat-menu-item]'));
    expect(items.map((item) => item.textContent?.trim())).toEqual(['Werkzeug löschen']);
    expect(rowClicked).not.toHaveBeenCalled();
  });

  it('should call the action of an entry', async () => {
    await openMenu();

    document.querySelector<HTMLElement>('[mat-menu-item]')?.click();

    expect(fixture.componentInstance.deleted).toHaveBeenCalledOnce();
    expect(rowClicked).not.toHaveBeenCalled();
  });
});
