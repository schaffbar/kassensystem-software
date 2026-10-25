import { Component, computed, inject, viewChild } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmDeleteComponent } from '../../../shared/components/confirm-delete/confirm-delete.component';
import { RfidTag } from '../../rfid-tag.model';
import { RfidTagStore } from '../../rfid-tags.store';

@Component({
  selector: 'schbar-rfid-tag-list',
  templateUrl: './rfid-tag-list.component.html',
  styleUrl: './rfid-tag-list.component.scss',
  imports: [MatButtonModule, MatTableModule, MatPaginatorModule, MatSortModule, MatIconModule, TranslatePipe],
})
export class RfidTagListComponent {
  private readonly store = inject(RfidTagStore);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

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
    this.store.loadAllRfidTags();
  }

  protected reloadRfidTags(): void {
    this.store.loadAllRfidTags();
  }

  protected deleteRfidTagDialog(event: Event, rfidTag: RfidTag): void {
    event.stopPropagation();

    if (!rfidTag || !rfidTag.id) {
      console.warn('No RFID tag selected for deletion');
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDeleteComponent, {
      data: {
        title: this.translate.instant('rfidTags.dialogs.deleteRfidTag.title'),
        entity: rfidTag.id,
        message: this.translate.instant('rfidTags.dialogs.deleteRfidTag.message'),
      },
      minWidth: '600px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.store.deleteRfidTag(rfidTag.id);
      }
    });
  }
}
