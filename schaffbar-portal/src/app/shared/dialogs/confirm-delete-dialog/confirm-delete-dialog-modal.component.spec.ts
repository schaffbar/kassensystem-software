import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  ConfirmDeleteDialogModalComponent,
  ConfirmDeleteDialogViewModel,
} from './confirm-delete-dialog-modal.component';

describe('ConfirmDeleteDialogModalComponent', () => {
  let fixture: ComponentFixture<ConfirmDeleteDialogModalComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ConfirmDeleteDialogModalComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('de', {
      tools: { dialogs: { deleteTool: { title: 'Werkzeug löschen', message: 'Wirklich löschen?' } } },
      shared: { dialogs: { actions: { delete: { confirm: 'Löschen', cancel: 'Abbrechen' } } } },
      custom: { confirm: 'Entfernen' },
    });
    translate.use('de');

    fixture = TestBed.createComponent(ConfirmDeleteDialogModalComponent);
  });

  async function render(viewModel: ConfirmDeleteDialogViewModel): Promise<void> {
    fixture.componentRef.setInput('viewModel', viewModel);
    await fixture.whenStable();
  }

  function texts(selector: string): string[] {
    return Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll(selector)).map(
      (element) => element.textContent?.trim() ?? '',
    );
  }

  function button(index: number): HTMLButtonElement {
    return fixture.nativeElement.querySelectorAll('button')[index];
  }

  const deleteTool: ConfirmDeleteDialogViewModel = {
    title: 'tools.dialogs.deleteTool.title',
    message: 'tools.dialogs.deleteTool.message',
  };

  it('should translate the title and message keys and show the entity as plain text', async () => {
    await render({ ...deleteTool, entity: 'Bandsäge' });

    expect(texts('h2')).toEqual(['Werkzeug löschen']);
    expect(texts('mat-dialog-content')[0]).toBe('Wirklich löschen? (Bandsäge)');
  });

  it('should use the default confirm label', async () => {
    await render(deleteTool);

    expect(texts('button')).toEqual(['Abbrechen', 'Löschen']);
  });

  it('should translate a custom confirm label', async () => {
    await render({ ...deleteTool, confirmLabel: 'custom.confirm' });

    expect(texts('button')).toEqual(['Abbrechen', 'Entfernen']);
  });

  it('should emit on confirm', async () => {
    const confirmed = vi.fn();
    fixture.componentInstance.confirmed.subscribe(confirmed);
    await render(deleteTool);

    button(1).click();

    expect(confirmed).toHaveBeenCalledOnce();
  });

  it('should emit on cancel', async () => {
    const cancelled = vi.fn();
    fixture.componentInstance.cancelled.subscribe(cancelled);
    await render(deleteTool);

    button(0).click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
