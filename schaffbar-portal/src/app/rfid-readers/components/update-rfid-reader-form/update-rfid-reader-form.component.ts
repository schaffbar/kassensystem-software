import { Component, computed, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, submit, validate } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { UpdateRfidReaderCommand } from '../../rfid-reader.model';
import { RfidReaderTypeKeyPipe } from '../../rfid-reader-type-key.pipe';

export interface UpdateRfidReaderViewModel {
  id: string;
  name: string | null;
  socketName: string | null;
  macAddress: string;
  type: RfidReaderType | null;
}

export interface UpdateRfidReaderDialogData {
  rfidReader: UpdateRfidReaderViewModel;
}

interface UpdateRfidReaderFormModel {
  name: string;
  socketName: string;
}

const SOCKET_NAME_PATTERN = /^[a-z0-9-]+$/;

@Component({
  selector: 'schbar-update-rfid-reader-form',
  templateUrl: './update-rfid-reader-form.component.html',
  styleUrl: './update-rfid-reader-form.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    FormDialogHeaderComponent,
    MatInputModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    TranslatePipe,
    RfidReaderTypeKeyPipe,
  ],
  host: { class: 'form-dialog' },
})
export class UpdateRfidReaderFormComponent {
  protected readonly data: UpdateRfidReaderDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UpdateRfidReaderFormComponent, UpdateRfidReaderCommand>);

  private readonly model = signal<UpdateRfidReaderFormModel>(toUpdateRfidReaderFormModel(this.data.rfidReader));

  protected readonly rfidReaderForm = form(this.model, (schema) => {
    // only a changed socket name is checked – existing readers may have older names, e.g. "Socket 43"
    validate(schema.socketName, ({ value }) => {
      const socketName = value();
      if (!socketName || socketName === (this.data.rfidReader.socketName ?? '')) return undefined;
      return SOCKET_NAME_PATTERN.test(socketName)
        ? undefined
        : { kind: 'socketNameFormat', message: 'rfidReaders.dialogs.updateRfidReader.errors.socketNameFormat' };
    });
  });

  // the device keeps connecting with its old socket name until someone changes it there as well
  protected readonly changedSocketName = computed(() => {
    const field = this.rfidReaderForm.socketName();
    const socketName = field.value();
    return socketName && field.valid() && socketName !== (this.data.rfidReader.socketName ?? '') ? socketName : null;
  });

  // --------------------------------------------------------------------------
  // actions

  protected saveRfidReader(): void {
    submit(this.rfidReaderForm, async () => {
      const { name, socketName } = this.model();
      const command: UpdateRfidReaderCommand = {
        rfidReaderId: this.data.rfidReader.id,
        name: name || undefined,
        socketName: socketName || undefined,
      };
      this.dialogRef.close(command);
    });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toUpdateRfidReaderFormModel(rfidReader: UpdateRfidReaderViewModel): UpdateRfidReaderFormModel {
  return { name: rfidReader.name ?? '', socketName: rfidReader.socketName ?? '' };
}
