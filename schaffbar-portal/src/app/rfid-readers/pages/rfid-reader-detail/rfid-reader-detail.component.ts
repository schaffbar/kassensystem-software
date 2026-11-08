import { Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { DangerZoneComponent } from '../../../shared/components/danger-zone/danger-zone.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { SNACKBAR_DURATION_MS } from '../../../shared/snack-bar/snack-bar-constants';
import { RfidReaderDetailInfoComponent } from '../../components/rfid-reader-detail-info/rfid-reader-detail-info.component';
import { RfidReaderDetailTypeComponent } from '../../components/rfid-reader-detail-type/rfid-reader-detail-type.component';
import {
  UpdateRfidReaderDialogData,
  UpdateRfidReaderFormComponent,
} from '../../components/update-rfid-reader-form/update-rfid-reader-form.component';
import { ChangeRfidReaderTypeCommand, DeleteRfidReaderCommand, UpdateRfidReaderCommand } from '../../rfid-reader.model';
import { RfidReaderTypeIconPipe } from '../../rfid-reader-type-icon.pipe';
import { RfidReaderTypeKeyPipe } from '../../rfid-reader-type-key.pipe';
import { RfidReaderDetailStore } from './rfid-reader-detail.store';

@Component({
  selector: 'schbar-rfid-reader-detail',
  templateUrl: './rfid-reader-detail.component.html',
  styleUrl: './rfid-reader-detail.component.scss',
  imports: [
    RfidReaderDetailInfoComponent,
    RfidReaderDetailTypeComponent,
    PageHeaderComponent,
    StatusBadgeComponent,
    RowMenuComponent,
    DangerZoneComponent,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    TranslatePipe,
    RfidReaderTypeKeyPipe,
    RfidReaderTypeIconPipe,
  ],
  providers: [RfidReaderDetailStore],
})
export class RfidReaderDetailComponent {
  protected readonly detailsStore = inject(RfidReaderDetailStore);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();

  protected readonly rfidReadersLink = `/${ROUTE.RFID_READERS}`;

  constructor() {
    this.detailsStore.setRfidReaderId(this.id);
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadRfidReader(): void {
    this.detailsStore.reloadRfidReader();
  }

  protected editRfidReader(): void {
    const rfidReader = this.detailsStore.rfidReader();
    if (!rfidReader) return;

    const dialogRef = this.dialog.open<
      UpdateRfidReaderFormComponent,
      UpdateRfidReaderDialogData,
      UpdateRfidReaderCommand
    >(UpdateRfidReaderFormComponent, {
      ...FORM_DIALOG_CONFIG,
      minWidth: DIALOG_WIDTH.medium,
      data: { rfidReader },
    });

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.detailsStore.updateRfidReader(command);
      }
    });
  }

  protected changeRfidReaderType(command: ChangeRfidReaderTypeCommand): void {
    this.detailsStore
      .canChangeRfidReaderType()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((canChange) => {
        if (canChange) {
          this.detailsStore.changeRfidReaderType(command);
        } else {
          this.showSwitchBoxHasAssignedTool();
        }
      });
  }

  protected deleteRfidReader(): void {
    const rfidReader = this.detailsStore.rfidReader();
    if (!rfidReader) return;

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
          this.detailsStore.deleteRfidReader(command);
        }
      });
  }

  // --------------------------------------------------------------------------
  // helper

  private showSwitchBoxHasAssignedTool(): void {
    this.snackBar.open(
      this.translate.instant('rfidReaders.dialogs.changeRfidReaderType.errors.switchBoxHasAssignedTool'),
      this.translate.instant('shared.actions.close'),
      { duration: SNACKBAR_DURATION_MS },
    );
  }
}
