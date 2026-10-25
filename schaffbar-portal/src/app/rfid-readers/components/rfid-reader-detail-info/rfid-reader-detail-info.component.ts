import { LowerCasePipe } from '@angular/common';
import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, FormRoot, readonly, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { RfidReader, UpdateRfidReaderCommand } from '../../rfid-reader.model';

interface RfidReaderInfoModel {
  name: string;
  socketName: string;
}

@Component({
  selector: 'schbar-rfid-reader-detail-info',
  templateUrl: './rfid-reader-detail-info.component.html',
  styleUrl: './rfid-reader-detail-info.component.scss',
  imports: [FormField, FormRoot, MatInputModule, MatButtonModule, MatIconModule, TranslatePipe, LowerCasePipe],
})
export class RfidReaderDetailInfoComponent {
  readonly rfidReader = input.required<RfidReader>();

  readonly readerChanged = output<UpdateRfidReaderCommand>();

  protected readonly isEditing = signal(false);

  // follows the rfidReader input
  private readonly model = linkedSignal(() => toModel(this.rfidReader()));

  protected readonly readerForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
  });

  protected onEdit(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.readerForm.name().focusBoundControl();
  }

  protected onCancel(): void {
    this.model.set(toModel(this.rfidReader()));
    this.readerForm().reset();
    this.isEditing.set(false);
  }

  protected onSave(): void {
    submit(this.readerForm, async () => {
      const { name, socketName } = this.model();
      const command: UpdateRfidReaderCommand = {
        id: this.rfidReader().id,
        name: name || undefined,
        socketName: socketName || undefined,
      };
      this.readerChanged.emit(command);
      this.isEditing.set(false);
    });
  }
}

function toModel(reader: RfidReader): RfidReaderInfoModel {
  return { name: reader.name ?? '', socketName: reader.socketName ?? '' };
}
