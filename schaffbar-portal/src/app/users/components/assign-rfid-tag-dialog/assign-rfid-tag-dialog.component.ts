import { Component, computed, input, output, signal } from '@angular/core';
import { form, FormField, FormRoot, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';

import { TranslatePipe } from '@ngx-translate/core';

import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { RfidTagIdPipe } from '../../../shared/pipes/rfid-tag-id.pipe';
import { RfidTagAssignmentTypeKeyPipe } from '../../rfid-tag-assignment-type-key.pipe';
import { AssignRfidTagCommand } from '../../rfid-tag-assignment.model';

export interface AssignRfidTagDialogViewModel {
  assignmentType: RfidTagAssignmentType;
  status: RfidTagAssignmentStatus;
  // null while waiting for the tag at the counter
  rfidTagId: string | null;
}

interface AssignRfidTagFormModel {
  assignmentType: RfidTagAssignmentType;
}

type Step = 'choose' | 'wait' | 'done';

const STEPS: Step[] = ['choose', 'wait', 'done'];

/**
 * Requests a tag and follows the request until the tag has been held to the reader at the counter. The page passes
 * the assignment of its store (reloaded every few seconds while waiting) and the pending state of the request with
 * `inputBinding`, and executes the command from `assignmentRequested` with `outputBinding`.
 */
@Component({
  selector: 'schbar-assign-rfid-tag-dialog',
  templateUrl: './assign-rfid-tag-dialog.component.html',
  styleUrl: './assign-rfid-tag-dialog.component.scss',
  imports: [
    FormField,
    FormRoot,
    FormDialogHeaderComponent,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatRadioModule,
    TranslatePipe,
    RfidTagIdPipe,
    RfidTagAssignmentTypeKeyPipe,
  ],
  host: { class: 'form-dialog' },
})
export class AssignRfidTagDialogComponent {
  readonly userId = input.required<string>();
  readonly userName = input.required<string>();
  // null = no tag and none requested
  readonly rfidTagAssignment = input.required<AssignRfidTagDialogViewModel | null>();
  readonly requestPending = input(false);

  readonly assignmentRequested = output<AssignRfidTagCommand>();

  protected readonly assignmentTypes = [RfidTagAssignmentType.Temporary, RfidTagAssignmentType.Fixed].map((type) => ({
    type,
    descriptionKey: `users.dialogs.assignRfidTag.descriptions.${type}`,
  }));

  private readonly model = signal<AssignRfidTagFormModel>({ assignmentType: RfidTagAssignmentType.Temporary });
  protected readonly assignmentForm = form(this.model);

  protected readonly steps = STEPS.map((step) => ({ step, labelKey: `users.dialogs.assignRfidTag.steps.${step}` }));

  // the page opens the dialog only without a tag or with a waiting request – an assigned tag is the result
  protected readonly step = computed<Step>(() => {
    const status = this.rfidTagAssignment()?.status;
    if (status === RfidTagAssignmentStatus.Assigned) return 'done';
    if (status === RfidTagAssignmentStatus.WaitingForAssignment || this.requestPending()) return 'wait';
    return 'choose';
  });
  protected readonly stepIndex = computed(() => STEPS.indexOf(this.step()));

  // --------------------------------------------------------------------------
  // actions

  protected requestRfidTag(): void {
    submit(this.assignmentForm, async () => {
      const command: AssignRfidTagCommand = { userId: this.userId(), assignmentType: this.model().assignmentType };
      this.assignmentRequested.emit(command);
    });
  }
}
