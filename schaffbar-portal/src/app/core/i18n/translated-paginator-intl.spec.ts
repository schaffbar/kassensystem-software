import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TranslatedPaginatorIntl } from './translated-paginator-intl';

describe('TranslatedPaginatorIntl', () => {
  let translate: TranslateService;
  let intl: TranslatedPaginatorIntl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideTranslateService(), TranslatedPaginatorIntl],
    });
    translate = TestBed.inject(TranslateService);
    translate.setTranslation('de', {
      shared: {
        paginator: {
          itemsPerPage: 'Einträge pro Seite',
          firstPage: 'Erste Seite',
          lastPage: 'Letzte Seite',
          nextPage: 'Nächste Seite',
          previousPage: 'Vorherige Seite',
          range: '{{start}} – {{end}} von {{length}}',
        },
      },
    });
    translate.use('de');
    intl = TestBed.inject(TranslatedPaginatorIntl);
  });

  it('should take the labels from the translations', () => {
    expect(intl.itemsPerPageLabel).toBe('Einträge pro Seite');
    expect(intl.nextPageLabel).toBe('Nächste Seite');
  });

  it('should show the range of the current page', () => {
    expect(intl.getRangeLabel(0, 15, 42)).toBe('1 – 15 von 42');
    expect(intl.getRangeLabel(2, 15, 42)).toBe('31 – 42 von 42');
  });

  it('should show an empty range for an empty list', () => {
    expect(intl.getRangeLabel(0, 15, 0)).toBe('0 – 0 von 0');
  });

  it('should render the paginators again when the language changes', () => {
    const changed = vi.fn();
    intl.changes.subscribe(changed);

    translate.setTranslation('en', { shared: { paginator: { itemsPerPage: 'Items per page' } } });
    translate.use('en');

    expect(intl.itemsPerPageLabel).toBe('Items per page');
    expect(changed).toHaveBeenCalled();
  });
});
