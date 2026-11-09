import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import {
  FilterChipsComponent,
  FilterChipViewModel,
} from '../../../shared/components/filter-chips/filter-chips.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { RfidTagStateBadgeComponent } from '../../components/rfid-tag-state-badge/rfid-tag-state-badge.component';
import {
  canDeleteRfidTag,
  DeleteRfidTagCommand,
  RfidTag,
  RfidTagState,
  UnassignRfidTagCommand,
} from '../../rfid-tag.model';
import { RfidTagAssignmentTypeKeyPipe } from '../../rfid-tag-assignment-type-key.pipe';
import { RfidTagIdPipe } from '../../../shared/pipes/rfid-tag-id.pipe';
import { RfidTagsStore } from '../../rfid-tags.store';

type RfidTagFilter = 'all' | RfidTagState;

const FILTERS: Record<RfidTagFilter, (rfidTag: RfidTag) => boolean> = {
  all: () => true,
  assigned: (rfidTag) => rfidTag.state === 'assigned',
  free: (rfidTag) => rfidTag.state === 'free',
  deactivated: (rfidTag) => rfidTag.state === 'deactivated',
};

@Component({
  selector: 'schbar-rfid-tag-list',
  templateUrl: './rfid-tag-list.component.html',
  styleUrl: './rfid-tag-list.component.scss',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    TranslatePipe,
    PageHeaderComponent,
    EmptyStateComponent,
    FilterChipsComponent,
    RowMenuComponent,
    RfidTagStateBadgeComponent,
    RfidTagAssignmentTypeKeyPipe,
    RfidTagIdPipe,
  ],
})
export class RfidTagListComponent {
  protected readonly store = inject(RfidTagsStore);
  private readonly translate = inject(TranslateService);
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  private readonly paginator = viewChild(MatPaginator);

  protected readonly searchText = signal('');
  protected readonly filter = signal<string>('all');

  protected readonly displayedColumns = ['id', 'state', 'assignee', 'actions'];
  protected readonly usersLink = `/${ROUTE.USERS}`;
  protected readonly canDeleteRfidTag = canDeleteRfidTag;

  private readonly filterLabels = toSignal<Record<string, string>>(
    this.translate.stream(Object.keys(FILTERS).map(toFilterLabelKey)),
  );

  protected readonly rfidTagsCount = computed(() => this.store.rfidTags().length);
  protected readonly isEmpty = computed(
    () => !this.store.isLoading() && !this.store.hasLoadError() && this.rfidTagsCount() === 0,
  );
  protected readonly subtitle = computed(() => {
    const rfidTags = this.store.rfidTags();
    return {
      key: rfidTags.length === 1 ? 'rfidTags.pages.rfidTagList.subtitleOne' : 'rfidTags.pages.rfidTagList.subtitle',
      params: {
        count: rfidTags.length,
        assigned: rfidTags.filter(FILTERS.assigned).length,
        free: rfidTags.filter(FILTERS.free).length,
        deactivated: rfidTags.filter(FILTERS.deactivated).length,
      },
    };
  });

  // the counts ignore the search, so they tell how many tags each filter has at all
  protected readonly filterChips = computed<FilterChipViewModel[]>(() =>
    (Object.keys(FILTERS) as RfidTagFilter[]).map((id) => ({
      id,
      label: this.filterLabels()?.[toFilterLabelKey(id)] ?? '',
      count: this.store.rfidTags().filter(FILTERS[id]).length,
    })),
  );

  private readonly shownRfidTags = computed(() => {
    const matchesFilter = FILTERS[this.filter() as RfidTagFilter] ?? FILTERS.all;
    const searchText = toComparable(this.searchText());
    return this.store
      .rfidTags()
      .filter(matchesFilter)
      .filter((rfidTag) => !searchText || toSearchText(rfidTag).includes(searchText));
  });
  protected readonly hasNoResults = computed(() => this.rfidTagsCount() > 0 && this.shownRfidTags().length === 0);

  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<RfidTag>(this.shownRfidTags());
    result.paginator = this.paginator() ?? null;
    return result;
  });

  constructor() {
    this.store.reloadRfidTags();
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadRfidTags(): void {
    this.store.reloadRfidTags();
  }

  protected searchRfidTags(searchText: string): void {
    this.searchText.set(searchText);
  }

  protected clearSearch(): void {
    this.searchText.set('');
  }

  protected unassignRfidTag(rfidTag: RfidTag): void {
    const assignee = rfidTag.assignee;
    if (!assignee) return;

    this.confirmActionDialog
      .open({
        title: 'rfidTags.dialogs.unassignRfidTag.title',
        message: 'rfidTags.dialogs.unassignRfidTag.message',
        confirmLabel: 'rfidTags.dialogs.unassignRfidTag.confirm',
        entity: assignee.userName,
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: UnassignRfidTagCommand = { rfidTagId: rfidTag.id, userId: assignee.userId };
          this.store.unassignRfidTag(command);
        }
      });
  }

  protected deleteRfidTag(rfidTag: RfidTag): void {
    if (!canDeleteRfidTag(rfidTag)) return;

    this.confirmDeleteDialog
      .open({
        title: 'rfidTags.dialogs.deleteRfidTag.title',
        entity: rfidTag.id,
        message: 'rfidTags.dialogs.deleteRfidTag.message',
        confirmLabel: 'rfidTags.dialogs.deleteRfidTag.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteRfidTagCommand = { rfidTagId: rfidTag.id };
          this.store.deleteRfidTag(command);
        }
      });
  }
}

// ----------------------------------------------------------------------------
// helper

function toFilterLabelKey(filter: string): string {
  return `rfidTags.pages.rfidTagList.filters.${filter}`;
}

// the ID is shown in groups ("04A3 1F9C") – searching works with and without the spaces
function toComparable(text: string): string {
  return text.toLowerCase().replace(/\s+/g, '');
}

function toSearchText(rfidTag: RfidTag): string {
  return toComparable(`${rfidTag.id}${rfidTag.assignee?.userName ?? ''}`);
}
