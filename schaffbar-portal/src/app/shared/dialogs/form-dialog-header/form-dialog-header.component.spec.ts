import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatDialogRef } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { FormDialogHeaderComponent } from './form-dialog-header.component';

describe('FormDialogHeaderComponent', () => {
  let fixture: ComponentFixture<FormDialogHeaderComponent>;
  let dialogRef: { close: Mock };

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [FormDialogHeaderComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FormDialogHeaderComponent);
    fixture.componentRef.setInput('title', 'Lesegerät bearbeiten');
    await fixture.whenStable();
  });

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  it('should show the title as dialog title and no subtitle by default', () => {
    expect(query('h2[mat-dialog-title]')?.textContent?.trim()).toBe('Lesegerät bearbeiten');
    expect(query('.subtitle')).toBeNull();
  });

  it('should show the subtitle', async () => {
    fixture.componentRef.setInput('subtitle', 'Tor Ausgang · Türwächter Ausgang');
    await fixture.whenStable();

    expect(query('.subtitle')?.textContent?.trim()).toBe('Tor Ausgang · Türwächter Ausgang');
  });

  it('should close the dialog without result', () => {
    const close = query('button');
    expect(close?.getAttribute('aria-label')).toBe('shared.actions.closeDialog');

    close?.click();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
