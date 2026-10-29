import { Component, computed, inject, viewChild } from '@angular/core';
import { Router } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { ConfirmDialogService } from '../../../shared/components/confirm-delete/confirm-dialog.service';
import { DeleteRfidReaderCommand, RfidReader } from '../../rfid-reader.model';
import { RfidReaderTypeKeyPipe } from '../../rfid-reader-type-key.pipe';
import { RfidReadersStore } from '../../rfid-readers.store';

@Component({
  selector: 'schbar-rfid-reader-list',
  templateUrl: './rfid-reader-list.component.html',
  styleUrl: './rfid-reader-list.component.scss',
  imports: [
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatIconModule,
    TranslatePipe,
    RfidReaderTypeKeyPipe,
  ],
})
export class RfidReaderListComponent {
  protected readonly store = inject(RfidReadersStore);
  private readonly router = inject(Router);
  private readonly confirmDialog = inject(ConfirmDialogService);

  private readonly paginator = viewChild(MatPaginator);
  private readonly sort = viewChild(MatSort);

  protected readonly displayedColumns = ['macAddress', 'name', 'type', 'actions'];
  protected readonly rfidReadersCount = computed(() => this.store.entities().length);
  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<RfidReader>(this.store.entities());
    result.sort = this.sort() ?? null;
    result.paginator = this.paginator() ?? null;
    return result;
  });

  constructor() {
    this.store.reloadRfidReaders();
  }

  protected reloadRfidReaders(): void {
    this.store.reloadRfidReaders();
  }

  protected navigateToDetail(rfidReader: RfidReader): void {
    this.router.navigate([ROUTE.RFID_READERS, rfidReader.id]);
  }

  protected deleteRfidReaderDialog(event: Event, rfidReader: RfidReader): void {
    event.stopPropagation();

    if (!rfidReader || !rfidReader.id) {
      console.warn('No RFID reader selected for deletion');
      return;
    }

    this.confirmDialog
      .confirm({
        title: 'rfidReaders.dialogs.deleteRfidReader.title',
        entity: rfidReader.name || rfidReader.macAddress,
        message: 'rfidReaders.dialogs.deleteRfidReader.message',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteRfidReaderCommand = { id: rfidReader.id };
          this.store.deleteRfidReader(command);
        }
      });
  }
}
