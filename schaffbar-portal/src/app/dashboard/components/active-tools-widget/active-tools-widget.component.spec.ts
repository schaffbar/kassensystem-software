import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ActiveToolsWidgetComponent, ToolStatusRowViewModel } from './active-tools-widget.component';

describe('ActiveToolsWidgetComponent', () => {
  const toolStatuses: ToolStatusRowViewModel[] = [
    { name: 'Bandsäge', usedBy: null },
    { name: 'Drechselbank', usedBy: { firstName: 'Max', lastName: 'Mustermann' } },
  ];

  let fixture: ComponentFixture<ActiveToolsWidgetComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActiveToolsWidgetComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ActiveToolsWidgetComponent);
    fixture.componentRef.setInput('toolStatuses', toolStatuses);
    await fixture.whenStable();
  });

  function text(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.trim() ?? '');
  }

  it('should count the tools in use', () => {
    expect(text('mat-card-title')[0]).toContain('(1/2)');
  });

  it('should list the tools in use first, with their user', () => {
    expect(text('td.mat-column-name')).toEqual(['Drechselbank', 'Bandsäge']);
    expect(text('td.mat-column-user')).toEqual(['Mustermann, Max', '']);
    expect(fixture.nativeElement.querySelectorAll('.status-circle.in-use').length).toBe(1);
  });

  it('should emit reload', () => {
    const reloadRequested = vi.fn();
    fixture.componentInstance.reloadRequested.subscribe(reloadRequested);

    fixture.nativeElement.querySelector('mat-card-header button').click();

    expect(reloadRequested).toHaveBeenCalledOnce();
  });
});
