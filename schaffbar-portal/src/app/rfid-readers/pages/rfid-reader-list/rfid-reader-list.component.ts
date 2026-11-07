import { Component, computed, inject, viewChild } from '@angular/core';
import { Router } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { DeleteRfidReaderCommand, RfidReader } from '../../rfid-reader.model';
import { RfidReaderTypeKeyPipe } from '../../rfid-reader-type-key.pipe';
import { RfidReadersStore } from '../../rfid-readers.store';

@Component({
  selector: 'schbar-rfid-reader-list',
  templateUrl: './rfid-reader-list.component.html',
  styleUrl: './rfid-reader-list.component.scss',
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatMenuModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatIconModule,
    TranslatePipe,
    RfidReaderTypeKeyPipe,
    OrDashPipe,
    PageHeaderComponent,
    EmptyStateComponent,
    RowMenuComponent,
    StatusBadgeComponent,
  ],
})
export class RfidReaderListComponent {
  protected readonly store = inject(RfidReadersStore);
  private readonly router = inject(Router);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  private readonly paginator = viewChild(MatPaginator);
  private readonly sort = viewChild(MatSort);

  protected readonly displayedColumns = ['name', 'type', 'macAddress', 'actions'];
  protected readonly rfidReadersCount = computed(() => this.store.rfidReaders().length);
  protected readonly isEmpty = computed(
    () => !this.store.isLoading() && !this.store.hasLoadError() && this.rfidReadersCount() === 0,
  );
  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<RfidReader>(this.store.rfidReaders());
    result.sort = this.sort() ?? null;
    result.paginator = this.paginator() ?? null;
    return result;
  });

  constructor() {
    this.store.reloadRfidReaders();
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadRfidReaders(): void {
    this.store.reloadRfidReaders();
  }

  protected openRfidReaderDetail(rfidReader: RfidReader): void {
    this.router.navigate([ROUTE.RFID_READERS, rfidReader.id]);
  }

  protected deleteRfidReader(rfidReader: RfidReader): void {
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
