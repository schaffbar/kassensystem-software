import { Component, input, linkedSignal, output } from '@angular/core';
import { form, FormField, FormRoot, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { ChangeRfidReaderTypeCommand } from '../../rfid-reader.model';
import { RfidReaderTypeDescriptionKeyPipe } from '../../rfid-reader-type-description-key.pipe';
import { RfidReaderTypeKeyPipe } from '../../rfid-reader-type-key.pipe';

export interface RfidReaderDetailTypeViewModel {
  id: string;
  // null for a new reader – someone has to set it
  type: RfidReaderType | null;
}

interface RfidReaderTypeFormModel {
  // '' = nothing selected yet
  type: RfidReaderType | '';
}

@Component({
  selector: 'schbar-rfid-reader-detail-type',
  templateUrl: './rfid-reader-detail-type.component.html',
  styleUrl: './rfid-reader-detail-type.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    TranslatePipe,
    RfidReaderTypeKeyPipe,
    RfidReaderTypeDescriptionKeyPipe,
  ],
  host: { class: 'card' },
})
export class RfidReaderDetailTypeComponent {
  readonly rfidReaderDetailType = input.required<RfidReaderDetailTypeViewModel>();

  // the selection stays open until the new type arrives – the page may still refuse the change
  readonly typeChangeRequested = output<ChangeRfidReaderTypeCommand>();

  protected readonly rfidReaderTypes = Object.values(RfidReaderType);

  // ends editing once the reader has its new type
  protected readonly isEditing = linkedSignal({
    source: () => this.rfidReaderDetailType().type,
    computation: () => false,
  });

  private readonly model = linkedSignal(() => toRfidReaderTypeFormModel(this.rfidReaderDetailType()));

  protected readonly rfidReaderTypeForm = form(this.model, (schema) => {
    required(schema.type);
  });

  // --------------------------------------------------------------------------
  // actions

  protected editRfidReaderType(): void {
    this.isEditing.set(true);
  }

  protected discardRfidReaderTypeChanges(): void {
    this.model.set(toRfidReaderTypeFormModel(this.rfidReaderDetailType()));
    this.rfidReaderTypeForm().reset();
    this.isEditing.set(false);
  }

  protected saveRfidReaderType(): void {
    submit(this.rfidReaderTypeForm, async () => {
      const { type } = this.model();
      const rfidReader = this.rfidReaderDetailType();
      if (!type) return;

      if (type === rfidReader.type) {
        this.isEditing.set(false);
        return;
      }
      const command: ChangeRfidReaderTypeCommand = { rfidReaderId: rfidReader.id, type };
      this.typeChangeRequested.emit(command);
    });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toRfidReaderTypeFormModel(rfidReader: RfidReaderDetailTypeViewModel): RfidReaderTypeFormModel {
  return { type: rfidReader.type ?? '' };
}
