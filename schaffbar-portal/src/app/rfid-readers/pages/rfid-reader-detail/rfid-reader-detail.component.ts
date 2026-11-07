import { Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { DIALOG_WIDTH } from '../../../shared/dialogs/dialog-constants';
import { SNACKBAR_DURATION_MS } from '../../../shared/snack-bar/snack-bar-constants';
import {
  ChangeRfidReaderTypeDialogData,
  ChangeRfidReaderTypeFormComponent,
} from '../../components/change-rfid-reader-type-form/change-rfid-reader-type-form.component';
import { RfidReaderDetailInfoComponent } from '../../components/rfid-reader-detail-info/rfid-reader-detail-info.component';
import { ChangeRfidReaderTypeCommand, RfidReader, UpdateRfidReaderCommand } from '../../rfid-reader.model';
import { RfidReaderDetailStore } from './rfid-reader-detail.store';

@Component({
  selector: 'schbar-rfid-reader-detail',
  templateUrl: './rfid-reader-detail.component.html',
  styleUrl: './rfid-reader-detail.component.scss',
  imports: [RfidReaderDetailInfoComponent, MatButtonModule, MatIconModule, MatProgressBarModule, TranslatePipe],
  providers: [RfidReaderDetailStore],
})
export class RfidReaderDetailComponent {
  protected readonly detailsStore = inject(RfidReaderDetailStore);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();

  constructor() {
    this.detailsStore.setRfidReaderId(this.id);
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadRfidReader(): void {
    this.detailsStore.reloadRfidReader();
  }

  protected updateRfidReader(command: UpdateRfidReaderCommand): void {
    this.detailsStore.updateRfidReader(command);
  }

  protected changeRfidReaderType(): void {
    const reader = this.detailsStore.rfidReader();
    if (!reader) return;

    this.detailsStore
      .canChangeRfidReaderType()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((canChange) => {
        if (canChange) {
          this.openChangeTypeDialog(reader);
        } else {
          this.snackBar.open(
            this.translate.instant('rfidReaders.dialogs.changeRfidReaderType.errors.switchBoxHasAssignedTool'),
            this.translate.instant('shared.actions.close'),
            { duration: SNACKBAR_DURATION_MS },
          );
        }
      });
  }

  // --------------------------------------------------------------------------
  // helper

  private openChangeTypeDialog(rfidReader: RfidReader): void {
    const dialogRef = this.dialog.open<
      ChangeRfidReaderTypeFormComponent,
      ChangeRfidReaderTypeDialogData,
      ChangeRfidReaderTypeCommand
    >(ChangeRfidReaderTypeFormComponent, {
      minWidth: DIALOG_WIDTH.small,
      disableClose: true,
      data: { rfidReader },
    });

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.detailsStore.changeRfidReaderType(command);
      }
    });
  }
}
