import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { ToolUsageSummaryComponent, ToolUsageSummaryViewModel } from './tool-usage-summary.component';

describe('ToolUsageSummaryComponent', () => {
  const summary: ToolUsageSummaryViewModel = {
    toolName: 'Bandsäge',
    active: true,
    totalDurationInMinutes: 45,
    totalUnits: 3,
    usages: [
      {
        startTime: new Date('2026-01-01T10:00:00Z'),
        endTime: new Date('2026-01-01T10:45:00Z'),
        durationInMinutes: 45,
        unitsUsed: 3,
      },
      { startTime: new Date('2026-01-01T11:00:00Z'), endTime: null, durationInMinutes: null, unitsUsed: null },
    ],
  };

  let fixture: ComponentFixture<ToolUsageSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolUsageSummaryComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolUsageSummaryComponent);
    fixture.componentRef.setInput('summary', summary);
    await fixture.whenStable();
  });

  function text(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.trim() ?? '');
  }

  it('should show the tool name, the active chip and the totals', () => {
    expect(text('.tool-summary-header')[0]).toContain('Bandsäge');
    expect(text('mat-chip')).toEqual(['users.chips.openSession.toolUsage.active']);
    expect(text('.tool-summary-totals')[0]).toContain('45');
    expect(text('.tool-summary-totals')[0]).toContain('3');
  });

  it('should show a dash for the values of a running usage', () => {
    expect(text('td.mat-column-endTime')[1]).toBe('–');
    expect(text('td.mat-column-durationInMinutes')).toEqual(['45', '–']);
    expect(text('td.mat-column-unitsUsed')).toEqual(['3', '–']);
  });

  it('should show a dash for a deleted tool and no chip when inactive', async () => {
    fixture.componentRef.setInput('summary', { ...summary, toolName: null, active: false });
    await fixture.whenStable();

    expect(text('.tool-summary-header')[0]).toBe('–');
    expect(text('mat-chip')).toEqual([]);
  });

  it('should show no table without usages', async () => {
    fixture.componentRef.setInput('summary', { ...summary, usages: [] });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });
});
