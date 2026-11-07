import { inject, Service } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatPaginatorIntl } from '@angular/material/paginator';

import { TranslateService } from '@ngx-translate/core';

interface PaginatorTexts {
  itemsPerPage: string;
  firstPage: string;
  lastPage: string;
  nextPage: string;
  previousPage: string;
}

@Service({ autoProvided: false })
export class TranslatedPaginatorIntl extends MatPaginatorIntl {
  private readonly translate = inject(TranslateService);

  constructor() {
    super();
    // stream() emits again when the language changes; changes.next() makes the paginators render the new texts
    this.translate
      .stream('shared.paginator')
      .pipe(takeUntilDestroyed())
      .subscribe((texts: PaginatorTexts) => {
        this.itemsPerPageLabel = texts.itemsPerPage;
        this.firstPageLabel = texts.firstPage;
        this.lastPageLabel = texts.lastPage;
        this.nextPageLabel = texts.nextPage;
        this.previousPageLabel = texts.previousPage;
        this.changes.next();
      });
  }

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    const start = length === 0 ? 0 : page * pageSize + 1;
    const end = Math.min((page + 1) * pageSize, length);
    return this.translate.instant('shared.paginator.range', { start, end, length });
  };
}
