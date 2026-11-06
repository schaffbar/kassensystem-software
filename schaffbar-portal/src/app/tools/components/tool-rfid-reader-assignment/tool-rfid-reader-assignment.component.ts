import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { form, FormField, FormRoot, submit, validate } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ChangeRfidReaderCommand, ClearRfidReaderCommand } from '../../tool.model';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface ToolRfidReaderAssignmentViewModel {
  id: string;
  name: string;
  rfidReaderId: string | null;
}

// a reader that may be assigned to the tool
export interface RfidReaderOptionViewModel {
  id: string;
  name: string | null;
  isAssignedToOtherTool: boolean;
}

interface RfidReaderAssignmentFormModel {
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
    OrDashPipe,
  ],
})
export class ToolRfidReaderAssignmentComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly toolRfidReaderAssignment = input.required<ToolRfidReaderAssignmentViewModel>();
  readonly rfidReaders = input.required<RfidReaderOptionViewModel[]>();

  readonly rfidReaderChanged = output<ChangeRfidReaderCommand>();
  readonly rfidReaderCleared = output<ClearRfidReaderCommand>();

  private readonly model = linkedSignal(() => toRfidReaderAssignmentFormModel(this.toolRfidReaderAssignment()));

  protected readonly rfidReaderForm = form(this.model, (schema) => {
    validate(schema.rfidReaderId, ({ value }) =>
      this.isAssignedToOtherTool(value())
        ? { kind: 'alreadyAssigned', message: 'tools.pages.toolDetails.messages.alreadyAssignedError' }
        : undefined,
    );
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

  protected saveRfidReader(): void {
    submit(this.rfidReaderForm, async () => {
      const { rfidReaderId } = this.model();
      if (!rfidReaderId) return;

      const command: ChangeRfidReaderCommand = { toolId: this.toolRfidReaderAssignment().id, rfidReaderId };
      this.rfidReaderChanged.emit(command);
    });
  }

  protected clearRfidReader(): void {
    this.confirmActionDialog
      .open({
        title: 'tools.dialogs.clearRfidReader.title',
        message: 'tools.dialogs.clearRfidReader.message',
        entity: this.toolRfidReaderAssignment().name,
        confirmLabel: 'tools.dialogs.clearRfidReader.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: ClearRfidReaderCommand = { toolId: this.toolRfidReaderAssignment().id };
          this.rfidReaderCleared.emit(command);
        }
      });
  }

  // --------------------------------------------------------------------------
  // helper

  private isAssignedToOtherTool(readerId: string): boolean {
    return this.rfidReaders().some((reader) => reader.id === readerId && reader.isAssignedToOtherTool);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toRfidReaderAssignmentFormModel(tool: ToolRfidReaderAssignmentViewModel): RfidReaderAssignmentFormModel {
  return { rfidReaderId: tool.rfidReaderId ?? '' };
}
