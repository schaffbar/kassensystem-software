import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import {
  FilterChipsComponent,
  FilterChipViewModel,
} from '../../../shared/components/filter-chips/filter-chips.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { DeleteRfidReaderCommand, isUnassigned, rfidReaderGroup, RfidReaderListItem } from '../../rfid-reader.model';
import { RfidReaderTypeIconPipe } from '../../rfid-reader-type-icon.pipe';
import { RfidReaderTypeKeyPipe } from '../../rfid-reader-type-key.pipe';
import { RfidReadersStore } from '../../rfid-readers.store';

type RfidReaderFilter = 'all' | 'desk' | 'switchBox' | 'unassigned';

const FILTERS: Record<RfidReaderFilter, (rfidReader: RfidReaderListItem) => boolean> = {
  all: () => true,
  desk: (rfidReader) => rfidReaderGroup(rfidReader.type) === 'desk',
  switchBox: (rfidReader) => rfidReaderGroup(rfidReader.type) === 'switchBox',
  unassigned: isUnassigned,
};

@Component({
  selector: 'schbar-rfid-reader-list',
  templateUrl: './rfid-reader-list.component.html',
  styleUrl: './rfid-reader-list.component.scss',
  imports: [
    NgTemplateOutlet,
    RouterLink,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTableModule,
    TranslatePipe,
    RfidReaderTypeKeyPipe,
    RfidReaderTypeIconPipe,
    OrDashPipe,
    PageHeaderComponent,
    EmptyStateComponent,
    FilterChipsComponent,
    RowMenuComponent,
    StatusBadgeComponent,
  ],
})
export class RfidReaderListComponent {
  protected readonly store = inject(RfidReadersStore);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  protected readonly searchText = signal('');
  protected readonly filter = signal<string>('all');

  private readonly filterLabels = toSignal<Record<string, string>>(
    this.translate.stream(Object.keys(FILTERS).map(toFilterLabelKey)),
  );

  protected readonly switchBoxColumns = ['name', 'tool', 'macAddress', 'socketName', 'actions'];
  protected readonly toolsLink = `/${ROUTE.TOOLS}`;

  protected readonly rfidReadersCount = computed(() => this.store.rfidReaders().length);
  protected readonly subtitleKey = computed(() =>
    this.rfidReadersCount() === 1
      ? 'rfidReaders.pages.rfidReaderList.subtitleOne'
      : 'rfidReaders.pages.rfidReaderList.subtitle',
  );
  protected readonly isEmpty = computed(
    () => !this.store.isLoading() && !this.store.hasLoadError() && this.rfidReadersCount() === 0,
  );

  // the counts ignore the search, so they tell how many readers each filter has at all
  protected readonly filterChips = computed<FilterChipViewModel[]>(() =>
    (Object.keys(FILTERS) as RfidReaderFilter[]).map((id) => ({
      id,
      label: this.filterLabels()?.[toFilterLabelKey(id)] ?? '',
      count: this.store.rfidReaders().filter(FILTERS[id]).length,
    })),
  );

  private readonly shownRfidReaders = computed(() => {
    const matchesFilter = FILTERS[this.filter() as RfidReaderFilter] ?? FILTERS.all;
    const searchText = this.searchText().trim().toLowerCase();
    return this.store
      .rfidReaders()
      .filter(matchesFilter)
      .filter((rfidReader) => !searchText || toSearchText(rfidReader).includes(searchText))
      .sort(byName);
  });

  protected readonly newRfidReaders = computed(() =>
    this.shownRfidReaders().filter((rfidReader) => rfidReaderGroup(rfidReader.type) === 'new'),
  );
  protected readonly deskRfidReaders = computed(() =>
    this.shownRfidReaders().filter((rfidReader) => rfidReaderGroup(rfidReader.type) === 'desk'),
  );
  protected readonly switchBoxes = computed(() =>
    this.shownRfidReaders().filter((rfidReader) => rfidReaderGroup(rfidReader.type) === 'switchBox'),
  );
  protected readonly hasNoResults = computed(() => this.rfidReadersCount() > 0 && this.shownRfidReaders().length === 0);

  constructor() {
    this.store.reloadRfidReaders();
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadRfidReaders(): void {
    this.store.reloadRfidReaders();
  }

  protected searchRfidReaders(searchText: string): void {
    this.searchText.set(searchText);
  }

  protected clearSearch(): void {
    this.searchText.set('');
  }

  protected openRfidReaderDetail(rfidReader: RfidReaderListItem): void {
    this.router.navigate([ROUTE.RFID_READERS, rfidReader.id]);
  }

  protected deleteRfidReader(rfidReader: RfidReaderListItem): void {
    this.confirmDeleteDialog
      .open({
        title: 'rfidReaders.dialogs.deleteRfidReader.title',
        entity: rfidReader.name || rfidReader.macAddress,
        message: 'rfidReaders.dialogs.deleteRfidReader.message',
        confirmLabel: 'rfidReaders.dialogs.deleteRfidReader.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteRfidReaderCommand = { rfidReaderId: rfidReader.id };
          this.store.deleteRfidReader(command);
        }
      });
  }
}

// ----------------------------------------------------------------------------
// helper

function toFilterLabelKey(filter: string): string {
  return `rfidReaders.pages.rfidReaderList.filters.${filter}`;
}

function toSearchText(rfidReader: RfidReaderListItem): string {
  return [rfidReader.name, rfidReader.macAddress, rfidReader.socketName, rfidReader.assignedTool?.name]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
}

// readers without name (new ones) by their MAC address
function byName(a: RfidReaderListItem, b: RfidReaderListItem): number {
  return (a.name || a.macAddress).localeCompare(b.name || b.macAddress, 'de');
}
