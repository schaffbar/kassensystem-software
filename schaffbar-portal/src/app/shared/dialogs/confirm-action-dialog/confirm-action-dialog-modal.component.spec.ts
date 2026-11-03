import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  ConfirmActionDialogModalComponent,
  ConfirmActionDialogViewModel,
} from './confirm-action-dialog-modal.component';

describe('ConfirmActionDialogModalComponent', () => {
  let fixture: ComponentFixture<ConfirmActionDialogModalComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ConfirmActionDialogModalComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('de', {
      pause: { title: 'Einweisung pausieren', message: 'Wirklich pausieren?', confirm: 'Pausieren' },
      shared: { dialogs: { confirmAction: { actions: { cancel: 'Abbrechen' } } } },
    });
    translate.use('de');

    fixture = TestBed.createComponent(ConfirmActionDialogModalComponent);
  });

  async function render(viewModel: ConfirmActionDialogViewModel): Promise<void> {
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

  const pause: ConfirmActionDialogViewModel = {
    title: 'pause.title',
    message: 'pause.message',
    confirmLabel: 'pause.confirm',
    destructive: false,
  };

  it('should translate the keys and show the entity as plain text', async () => {
    await render({ ...pause, entity: 'Bandsäge' });

    expect(texts('h2')).toEqual(['Einweisung pausieren']);
    expect(texts('mat-dialog-content')[0]).toBe('Wirklich pausieren? (Bandsäge)');
    expect(texts('button')).toEqual(['Abbrechen', 'Pausieren']);
  });

  it('should focus confirm and use a normal button for a non-destructive action', async () => {
    await render(pause);

    expect(button(0).hasAttribute('cdkFocusInitial')).toBe(false);
    expect(button(1).hasAttribute('cdkFocusInitial')).toBe(true);
    expect(button(1).classList).not.toContain('danger-button');
  });

  it('should focus cancel and use a danger button for a destructive action', async () => {
    await render({ ...pause, destructive: true });

    expect(button(0).hasAttribute('cdkFocusInitial')).toBe(true);
    expect(button(1).hasAttribute('cdkFocusInitial')).toBe(false);
    expect(button(1).classList).toContain('danger-button');
  });

  it('should emit on confirm', async () => {
    const confirmed = vi.fn();
    fixture.componentInstance.confirmed.subscribe(confirmed);
    await render(pause);

    button(1).click();

    expect(confirmed).toHaveBeenCalledOnce();
  });

  it('should emit on cancel', async () => {
    const cancelled = vi.fn();
    fixture.componentInstance.cancelled.subscribe(cancelled);
    await render(pause);

    button(0).click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
