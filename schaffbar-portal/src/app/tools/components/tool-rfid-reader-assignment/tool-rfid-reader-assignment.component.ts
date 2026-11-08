import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { disabled, form, FormField, FormRoot, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ChangeRfidReaderCommand, ClearRfidReaderCommand } from '../../tool.model';

export interface ToolRfidReaderAssignmentViewModel {
  id: string;
  name: string;
  rfidReaderId: string | null;
  // false while the tool is running
  canClearRfidReader: boolean;
}

// a switch box that may be assigned to the tool
export interface RfidReaderOptionViewModel {
  id: string;
  name: string | null;
  macAddress: string;
}

interface RfidReaderAssignmentFormModel {
  // '' = no switch box assigned
  rfidReaderId: string;
}

@Component({
  selector: 'schbar-tool-rfid-reader-assignment',
  templateUrl: './tool-rfid-reader-assignment.component.html',
  styleUrl: './tool-rfid-reader-assignment.component.scss',
  imports: [FormField, FormRoot, MatButtonModule, MatSelectModule, TranslatePipe],
  host: { class: 'card' },
})
export class ToolRfidReaderAssignmentComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly toolRfidReaderAssignment = input.required<ToolRfidReaderAssignmentViewModel>();
  readonly rfidReaders = input.required<RfidReaderOptionViewModel[]>();

  readonly rfidReaderChanged = output<ChangeRfidReaderCommand>();
  readonly rfidReaderCleared = output<ClearRfidReaderCommand>();

  // follows the input – a saved change arrives as the new assignment
  private readonly model = linkedSignal(() => toRfidReaderAssignmentFormModel(this.toolRfidReaderAssignment()));

  protected readonly rfidReaderForm = form(this.model, (schema) => {
    // the tool's own switch box is always offered, so an empty list means: nothing to choose
    disabled(schema.rfidReaderId, () => this.rfidReaders().length === 0);
  });

  protected readonly isDirty = computed(
    () => this.model().rfidReaderId !== toRfidReaderAssignmentFormModel(this.toolRfidReaderAssignment()).rfidReaderId,
  );

  // --------------------------------------------------------------------------
  // actions

  protected discardRfidReaderChanges(): void {
    this.model.set(toRfidReaderAssignmentFormModel(this.toolRfidReaderAssignment()));
    this.rfidReaderForm().reset();
  }

  protected changeRfidReader(): void {
    submit(this.rfidReaderForm, async () => {
      const { rfidReaderId } = this.model();
      if (!rfidReaderId) return;

      const command: ChangeRfidReaderCommand = { toolId: this.toolRfidReaderAssignment().id, rfidReaderId };
      this.rfidReaderChanged.emit(command);
    });
  }

  protected clearRfidReader(): void {
    const tool = this.toolRfidReaderAssignment();
    if (!tool.canClearRfidReader) return;

    this.confirmActionDialog
      .open({
        title: 'tools.dialogs.clearRfidReader.title',
        message: 'tools.dialogs.clearRfidReader.message',
        entity: tool.name,
        confirmLabel: 'tools.dialogs.clearRfidReader.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: ClearRfidReaderCommand = { toolId: tool.id };
          this.rfidReaderCleared.emit(command);
        }
      });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toRfidReaderAssignmentFormModel(tool: ToolRfidReaderAssignmentViewModel): RfidReaderAssignmentFormModel {
  return { rfidReaderId: tool.rfidReaderId ?? '' };
}
