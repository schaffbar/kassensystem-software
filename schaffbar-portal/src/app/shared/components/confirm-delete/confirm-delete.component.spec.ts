import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MAT_DIALOG_DATA } from '@angular/material/dialog';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { describe, expect, it } from 'vitest';

import { ConfirmDeleteComponent, ConfirmDeleteDialogData } from './confirm-delete.component';

describe('ConfirmDeleteComponent', () => {
  function create(data: ConfirmDeleteDialogData): ComponentFixture<ConfirmDeleteComponent> {
    TestBed.configureTestingModule({
      imports: [ConfirmDeleteComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('de', {
      tools: { dialogs: { deleteTool: { title: 'Werkzeug löschen', message: 'Wirklich löschen?' } } },
      shared: { dialogs: { actions: { delete: { confirm: 'Löschen', cancel: 'Abbrechen' } } } },
      custom: { confirm: 'Entfernen' },
    });
    translate.use('de');

    const fixture = TestBed.createComponent(ConfirmDeleteComponent);
    fixture.detectChanges();
    return fixture;
  }

  const text = (fixture: ComponentFixture<ConfirmDeleteComponent>, selector: string): string[] =>
    Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll(selector)).map((el) => el.textContent?.trim() ?? '');

  it('should translate the title and message keys and show the entity as plain text', () => {
    const fixture = create({
      title: 'tools.dialogs.deleteTool.title',
      message: 'tools.dialogs.deleteTool.message',
      entity: 'Bandsäge',
    });

    expect(text(fixture, 'h2')).toEqual(['Werkzeug löschen']);
    expect(text(fixture, 'mat-dialog-content')[0]).toBe('Wirklich löschen? (Bandsäge)');
  });

  it('should use the default confirm label', () => {
    const fixture = create({ title: 'tools.dialogs.deleteTool.title', message: 'tools.dialogs.deleteTool.message' });

    expect(text(fixture, 'button')).toEqual(['Abbrechen', 'Löschen']);
  });

  it('should translate a custom confirm label', () => {
    const fixture = create({
      title: 'tools.dialogs.deleteTool.title',
      message: 'tools.dialogs.deleteTool.message',
      confirmLabel: 'custom.confirm',
    });

    expect(text(fixture, 'button')).toEqual(['Abbrechen', 'Entfernen']);
  });
});
