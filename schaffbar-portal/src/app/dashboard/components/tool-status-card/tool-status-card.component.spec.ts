import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { ToolArea } from '../../../shared/models/tool-area';
import { ALL_AREAS, ToolStatusCardComponent, ToolStatusRowViewModel } from './tool-status-card.component';

describe('ToolStatusCardComponent', () => {
  const tool = (name: string, area: ToolArea, usedBy: string | null = null): ToolStatusRowViewModel => ({
    toolId: `tool-${name}`,
    name,
    area,
    usedBy,
  });

  let fixture: ComponentFixture<ToolStatusCardComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ToolStatusCardComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideTranslateService()],
    });
    fixture = TestBed.createComponent(ToolStatusCardComponent);
    fixture.componentRef.setInput('areaSegments', [
      { id: ALL_AREAS, label: 'Alle' },
      { id: ToolArea.Holz, label: 'Holz' },
      { id: ToolArea.Metall, label: 'Metall' },
    ]);
  });

  async function render(tools: ToolStatusRowViewModel[]): Promise<void> {
    fixture.componentRef.setInput('toolStatuses', tools);
    await fixture.whenStable();
  }

  function names(): string[] {
    return [...fixture.nativeElement.querySelectorAll('li .name')].map((name: HTMLElement) => name.textContent!.trim());
  }

  it('should show the tools in use first, then by name, with their status', async () => {
    await render([
      tool('Hobel', ToolArea.Holz),
      tool('Bandsäge', ToolArea.Holz),
      tool('Drehbank', ToolArea.Metall, 'Anna Becker'),
    ]);

    expect(names()).toEqual(['Drehbank', 'Bandsäge', 'Hobel']);
    const first: HTMLElement = fixture.nativeElement.querySelector('li');
    expect(first.querySelector('.meta')!.textContent!.replace(/\s+/g, ' ').trim()).toBe(
      'shared.toolAreas.METALL · Anna Becker',
    );
    expect(first.querySelector('schbar-status-badge')!.textContent!.trim()).toBe(
      'play_circle dashboard.toolStatus.statuses.inUse',
    );
    expect(first.querySelector('a')!.getAttribute('href')).toBe('/tools/tool-Drehbank');
  });

  it('should filter by area', async () => {
    await render([tool('Hobel', ToolArea.Holz), tool('Drehbank', ToolArea.Metall)]);

    [...fixture.nativeElement.querySelectorAll('schbar-segmented-control button')]
      .find((button: HTMLElement) => button.textContent!.includes('Metall'))
      .click();
    await fixture.whenStable();

    expect(names()).toEqual(['Drehbank']);
  });

  it('should show at most eight tools and link to all of them', async () => {
    await render(Array.from({ length: 10 }, (_, index) => tool(`Werkzeug ${index}`, ToolArea.Holz)));

    expect(names()).toHaveLength(8);
    expect(fixture.nativeElement.querySelector('.all-tools').getAttribute('href')).toBe('/tools');
  });

  it('should show an empty state for an area without tools', async () => {
    await render([]);

    expect(fixture.nativeElement.querySelector('schbar-empty-state')).not.toBeNull();
  });
});
