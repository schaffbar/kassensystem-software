import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { form, FormField, FormRoot, submit, validate } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { RfidReader, RfidReaderType } from '../../../rfid-readers/rfid-reader.model';
import {
  ConfirmDeleteComponent,
  ConfirmDeleteDialogData,
} from '../../../shared/components/confirm-delete/confirm-delete.component';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { ChangeRfidReaderCommand, ClearRfidReaderCommand, Tool } from '../../tool.model';

interface RfidReaderAssignmentModel {
  // '' = no RFID reader assigned
  rfidReaderId: string;
}

@Component({
  selector: 'schbar-tool-rfid-reader-assignment',
  templateUrl: './tool-rfid-reader-assignment.component.html',
  styleUrl: './tool-rfid-reader-assignment.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TranslatePipe,
  ],
})
export class ToolRfidReaderAssignmentComponent {
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly tool = input.required<Tool>();
  readonly rfidReaders = input.required<RfidReader[]>();
  readonly assignedRfidReaderIds = input<Set<string>>(new Set());

  readonly rfidReaderChanged = output<ChangeRfidReaderCommand>();
  readonly rfidReaderCleared = output<ClearRfidReaderCommand>();

  private readonly model = linkedSignal(() => toModel(this.tool()));

  protected readonly rfidReaderForm = form(this.model, (schema) => {
    validate(schema.rfidReaderId, ({ value }) =>
      this.isAssigned(value())
        ? { kind: 'alreadyAssigned', message: 'tools.pages.toolDetails.messages.alreadyAssignedError' }
        : undefined,
    );
  });

  protected readonly isDirty = computed(() => this.model().rfidReaderId !== toModel(this.tool()).rfidReaderId);

  protected readonly filteredRfidReaders = computed(() => {
    return this.rfidReaders().filter((reader) => reader.type === RfidReaderType.SwitchBox);
  });

  protected isAssigned(readerId: string): boolean {
    return this.assignedRfidReaderIds().has(readerId);
  }

  protected onCancel(): void {
    this.model.set(toModel(this.tool()));
    this.rfidReaderForm().reset();
  }

  protected onSave(): void {
    submit(this.rfidReaderForm, async () => {
      const { rfidReaderId } = this.model();
      if (!rfidReaderId) return;

      const command: ChangeRfidReaderCommand = { toolId: this.tool().id, rfidReaderId };
      this.rfidReaderChanged.emit(command);
    });
  }

  protected onClear(): void {
    const dialogRef = this.dialog.open<ConfirmDeleteComponent, ConfirmDeleteDialogData, boolean>(
      ConfirmDeleteComponent,
      {
        data: {
          title: this.translate.instant('tools.dialogs.clearRfidReader.title'),
          message: this.translate.instant('tools.dialogs.clearRfidReader.message'),
          entity: this.tool().name,
        },
        minWidth: '600px',
        disableClose: true,
      },
    );

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        const command: ClearRfidReaderCommand = { toolId: this.tool().id };
        this.rfidReaderCleared.emit(command);
      }
    });
  }
}

function toModel(tool: Tool): RfidReaderAssignmentModel {
  return { rfidReaderId: tool.rfidReaderId ?? '' };
}
