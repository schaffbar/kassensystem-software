import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { WorkshopUsageRowViewModel, WorkshopUsageSummaryComponent } from './workshop-usage-summary.component';

describe('WorkshopUsageSummaryComponent', () => {
  const workshopUsages: WorkshopUsageRowViewModel[] = [
    {
      entryTime: new Date('2026-01-01T10:00:00Z'),
      exitTime: new Date('2026-01-01T12:00:00Z'),
      durationInMinutes: 120,
      unitsUsed: 4,
    },
    { entryTime: new Date('2026-01-01T13:00:00Z'), exitTime: null, durationInMinutes: null, unitsUsed: null },
  ];

  let fixture: ComponentFixture<WorkshopUsageSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkshopUsageSummaryComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkshopUsageSummaryComponent);
    fixture.componentRef.setInput('workshopUsageSummary', {
      workshopUsages,
      totalWorkshopDurationInMinutes: 120,
      totalWorkshopUnits: 4,
    });
    await fixture.whenStable();
  });

  function text(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.trim() ?? '');
  }

  it('should show one row per usage with a dash for the values of the current visit', () => {
    expect(text('td.mat-column-exitTime')[1]).toBe('–');
    expect(text('td.mat-column-durationInMinutes')).toEqual(['120', '–']);
    expect(text('td.mat-column-unitsUsed')).toEqual(['4', '–']);
  });

  it('should show the totals', () => {
    const totals = text('h3')[0];

    expect(totals).toContain('120');
    expect(totals).toContain('4');
  });
});
