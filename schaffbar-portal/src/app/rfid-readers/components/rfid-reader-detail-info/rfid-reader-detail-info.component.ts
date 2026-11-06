import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, FormRoot, readonly, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { UpdateRfidReaderCommand } from '../../rfid-reader.model';
import { RfidReaderTypeKeyPipe } from '../../rfid-reader-type-key.pipe';

export interface RfidReaderDetailInfoViewModel {
  id: string;
  macAddress: string;
  // null for a new reader – someone has to set it
  type: RfidReaderType | null;
  name: string | null;
  socketName: string | null;
}

interface RfidReaderInfoFormModel {
  name: string;
  socketName: string;
}

@Component({
  selector: 'schbar-rfid-reader-detail-info',
  templateUrl: './rfid-reader-detail-info.component.html',
  styleUrl: './rfid-reader-detail-info.component.scss',
  imports: [FormField, FormRoot, MatInputModule, MatButtonModule, MatIconModule, TranslatePipe, RfidReaderTypeKeyPipe],
})
export class RfidReaderDetailInfoComponent {
  readonly rfidReaderDetailInfo = input.required<RfidReaderDetailInfoViewModel>();

  readonly rfidReaderChanged = output<UpdateRfidReaderCommand>();

  protected readonly isEditing = signal(false);

  // follows the rfidReaderDetailInfo input
  private readonly model = linkedSignal(() => toRfidReaderInfoFormModel(this.rfidReaderDetailInfo()));

  protected readonly readerForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
  });

  // --------------------------------------------------------------------------
  // actions

  protected editRfidReader(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.readerForm.name().focusBoundControl();
  }

  protected discardRfidReaderChanges(): void {
    this.model.set(toRfidReaderInfoFormModel(this.rfidReaderDetailInfo()));
    this.readerForm().reset();
    this.isEditing.set(false);
  }

  protected saveRfidReader(): void {
    submit(this.readerForm, async () => {
      const { name, socketName } = this.model();
      const command: UpdateRfidReaderCommand = {
        id: this.rfidReaderDetailInfo().id,
        name: name || undefined,
        socketName: socketName || undefined,
      };
      this.rfidReaderChanged.emit(command);
      this.isEditing.set(false);
    });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toRfidReaderInfoFormModel(reader: RfidReaderDetailInfoViewModel): RfidReaderInfoFormModel {
  return { name: reader.name ?? '', socketName: reader.socketName ?? '' };
}
