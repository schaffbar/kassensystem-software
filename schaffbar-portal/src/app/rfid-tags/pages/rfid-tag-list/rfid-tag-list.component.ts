import { Component, computed, inject, viewChild } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { ConfirmDialogService } from '../../../shared/components/confirm-delete/confirm-dialog.service';
import { DeleteRfidTagCommand, RfidTag } from '../../rfid-tag.model';
import { RfidTagsStore } from '../../rfid-tags.store';

@Component({
  selector: 'schbar-rfid-tag-list',
  templateUrl: './rfid-tag-list.component.html',
  styleUrl: './rfid-tag-list.component.scss',
  imports: [
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatIconModule,
    TranslatePipe,
  ],
})
export class RfidTagListComponent {
  protected readonly store = inject(RfidTagsStore);
  private readonly confirmDialog = inject(ConfirmDialogService);

  private readonly paginator = viewChild(MatPaginator);
  private readonly sort = viewChild(MatSort);

  protected readonly displayedColumns = ['id', 'active', 'actions'];
  protected readonly rfidTagsCount = computed(() => this.store.entities().length);
  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<RfidTag>(this.store.entities());
    result.sort = this.sort() ?? null;
    result.paginator = this.paginator() ?? null;
    return result;
  });

  constructor() {
    this.store.reloadRfidTags();
  }

  protected reloadRfidTags(): void {
    this.store.reloadRfidTags();
  }

  protected deleteRfidTagDialog(event: Event, rfidTag: RfidTag): void {
    event.stopPropagation();

    if (!rfidTag || !rfidTag.id) {
      console.warn('No RFID tag selected for deletion');
      return;
    }

    this.confirmDialog
      .confirm({
        title: 'rfidTags.dialogs.deleteRfidTag.title',
        entity: rfidTag.id,
        message: 'rfidTags.dialogs.deleteRfidTag.message',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteRfidTagCommand = { id: rfidTag.id };
          this.store.deleteRfidTag(command);
        }
      });
  }
}
