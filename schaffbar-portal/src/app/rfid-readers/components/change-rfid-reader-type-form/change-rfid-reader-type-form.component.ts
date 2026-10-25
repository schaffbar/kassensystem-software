import { LowerCasePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { form, FormField, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { ChangeRfidReaderTypeCommand, RfidReader, RfidReaderType } from '../../rfid-reader.model';

export interface ChangeRfidReaderTypeDialogData {
  rfidReader: RfidReader;
}

interface ChangeRfidReaderTypeModel {
  // '' = nothing selected (a new reader comes with `type: null` from the backend)
  type: RfidReaderType | '';
}

@Component({
  selector: 'schbar-change-rfid-reader-type-form',
  templateUrl: './change-rfid-reader-type-form.component.html',
  styleUrl: './change-rfid-reader-type-form.component.scss',
  imports: [
    LowerCasePipe,
    FormField,
    FieldErrorComponent,
    MatSelectModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    TranslatePipe,
  ],
})
export class ChangeRfidReaderTypeFormComponent {
  protected readonly data: ChangeRfidReaderTypeDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ChangeRfidReaderTypeFormComponent>);

  protected readonly rfidReaderTypes = Object.values(RfidReaderType);

  private readonly model = signal<ChangeRfidReaderTypeModel>({ type: this.data.rfidReader.type ?? '' });

  protected readonly typeForm = form(this.model, (schema) => {
    required(schema.type);
  });

  protected save(): void {
    submit(this.typeForm, async () => {
      const { type } = this.model();
      if (!type) return;

      const command: ChangeRfidReaderTypeCommand = { id: this.data.rfidReader.id, type };
      this.dialogRef.close(command);
    });
  }
}
