import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { DashboardAttentionComponent } from './dashboard-attention.component';

describe('DashboardAttentionComponent', () => {
  let fixture: ComponentFixture<DashboardAttentionComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardAttentionComponent],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideTranslateService()],
    });
    // the parameters show up in the text
    TestBed.inject(TranslateService).setTranslation('de', {
      dashboard: {
        attention: {
          title: 'Braucht Aufmerksamkeit ({{count}})',
          rfidReadersWithoutType: {
            one: 'one {{name}}',
            other: 'other {{count}}: {{names}}',
            link: { one: 'Einrichten', other: 'Lesegeräte' },
          },
          toolsWithoutHardware: {
            one: 'one {{name}}',
            other: 'other {{count}}: {{names}}',
            link: { one: 'Hardware', other: 'Werkzeuge' },
          },
        },
      },
    });
    TestBed.inject(TranslateService).use('de');
    fixture = TestBed.createComponent(DashboardAttentionComponent);
  });

  function lines(): { text: string; link: string | null }[] {
    return [...fixture.nativeElement.querySelectorAll('li')].map((line: HTMLElement) => ({
      text: line.querySelector('span')!.textContent!.trim(),
      link: line.querySelector('a')!.getAttribute('href'),
    }));
  }

  it('should name a single entity and link to it', async () => {
    fixture.componentRef.setInput('items', [
      { kind: 'rfidReadersWithoutType', names: ['AA:BB'], link: ['/', 'rfid-readers', 'reader-1'] },
    ]);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('h2').textContent.trim()).toBe('warning Braucht Aufmerksamkeit (1)');
    expect(lines()).toEqual([{ text: 'one AA:BB', link: '/rfid-readers/reader-1' }]);
  });

  it('should list at most three names of several entities and link to their list', async () => {
    fixture.componentRef.setInput('items', [
      { kind: 'toolsWithoutHardware', names: ['Bandsäge', 'Hobel', 'Kappsäge', 'Kreissäge'], link: ['/', 'tools'] },
    ]);
    await fixture.whenStable();

    expect(lines()).toEqual([{ text: 'other 4: Bandsäge, Hobel, Kappsäge …', link: '/tools' }]);
  });
});
