import { Component, inject, input } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { take } from 'rxjs';

import { ToolsService } from '../../../tools/tools.service';
import {
  ChangeRfidReaderTypeDialogData,
  ChangeRfidReaderTypeFormComponent,
} from '../../components/change-rfid-reader-type-form/change-rfid-reader-type-form.component';
import { RfidReaderDetailInfoComponent } from '../../components/rfid-reader-detail-info/rfid-reader-detail-info.component';
import {
  ChangeRfidReaderTypeCommand,
  RfidReader,
  RfidReaderType,
  UpdateRfidReaderCommand,
} from '../../rfid-reader.model';
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
  private readonly toolsService = inject(ToolsService);
  private readonly translate = inject(TranslateService);

  readonly id = input.required<string>();

  constructor() {
    this.detailsStore.setRfidReaderId(this.id);
  }

  protected reloadRfidReader(): void {
    this.detailsStore.reloadRfidReader();
  }

  protected onRfidReaderChanged(command: UpdateRfidReaderCommand): void {
    this.detailsStore.updateRfidReader(command);
  }

  protected changeType(): void {
    const reader = this.detailsStore.rfidReader();
    if (!reader) return;

    if (reader.type === RfidReaderType.SwitchBox) {
      this.toolsService
        .getToolsByRfidReader(reader.id)
        .pipe(take(1))
        .subscribe((tools) => {
          if (tools.length > 0) {
            this.snackBar.open(
              this.translate.instant('rfidReaders.dialogs.changeRfidReaderType.errors.switchBoxHasAssignedTool'),
              this.translate.instant('shared.actions.close'),
              { duration: 6000 },
            );
          } else {
            this.openChangeTypeDialog(reader);
          }
        });
    } else {
      this.openChangeTypeDialog(reader);
    }
  }

  private openChangeTypeDialog(rfidReader: RfidReader): void {
    const dialogRef = this.dialog.open<
      ChangeRfidReaderTypeFormComponent,
      ChangeRfidReaderTypeDialogData,
      ChangeRfidReaderTypeCommand
    >(ChangeRfidReaderTypeFormComponent, {
      minWidth: '400px',
      disableClose: true,
      data: { rfidReader },
    });

    dialogRef
      .afterClosed()
      .pipe(take(1))
      .subscribe((command) => {
        if (command) {
          this.detailsStore.changeRfidReaderType(command);
        }
      });
  }
}
