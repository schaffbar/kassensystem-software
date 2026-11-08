import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { form, FormField, FormRoot, hidden, pattern, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { ClearWlanRelaisCommand, SetWlanRelaisCommand, wlanRelaisCommands } from '../../tool.model';
import { WlanRelaisTypeKeyPipe } from '../../wlan-relais-type-key.pipe';

const IP_PATTERN = /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/;

export interface ToolWlanRelaisViewModel {
  id: string;
  name: string;
  wlanRelaisType: WlanRelaisType | null;
  ipAddress: string | null;
}

interface WlanRelaisFormModel {
  // '' = no WLAN relais yet; removing a relais is its own action
  wlanRelaisType: WlanRelaisType | '';
  ipAddress: string;
}

@Component({
  selector: 'schbar-tool-wlan-relais',
  templateUrl: './tool-wlan-relais.component.html',
  styleUrl: './tool-wlan-relais.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    TranslatePipe,
    WlanRelaisTypeKeyPipe,
  ],
  host: { class: 'card' },
})
export class ToolWlanRelaisComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly toolWlanRelais = input.required<ToolWlanRelaisViewModel>();

  readonly wlanRelaisUpdated = output<SetWlanRelaisCommand>();
  readonly wlanRelaisCleared = output<ClearWlanRelaisCommand>();

  protected readonly wlanRelaisTypes = Object.values(WlanRelaisType);

  private readonly model = linkedSignal(() => toWlanRelaisFormModel(this.toolWlanRelais()));

  protected readonly wlanRelaisForm = form(this.model, (schema) => {
    // a hidden field does not count for the validity of the form, so the IP rules only apply with a relais type
    hidden(schema.ipAddress, { when: ({ valueOf }) => !valueOf(schema.wlanRelaisType) });
    required(schema.ipAddress);
    pattern(schema.ipAddress, IP_PATTERN, { message: 'tools.pages.toolDetails.errors.invalidIpAddress' });
  });

  // only for a valid IP address – otherwise the URLs would read `http:///…`
  protected readonly derivedCommands = computed(() => {
    const { wlanRelaisType, ipAddress } = this.model();
    return wlanRelaisType && this.wlanRelaisForm.ipAddress().valid()
      ? wlanRelaisCommands(wlanRelaisType, ipAddress)
      : null;
  });

  protected readonly isDirty = computed(() => {
    const current = this.model();
    const saved = toWlanRelaisFormModel(this.toolWlanRelais());
    return current.wlanRelaisType !== saved.wlanRelaisType || current.ipAddress !== saved.ipAddress;
  });

  // --------------------------------------------------------------------------
  // actions

  protected discardWlanRelaisChanges(): void {
    this.model.set(toWlanRelaisFormModel(this.toolWlanRelais()));
    this.wlanRelaisForm().reset();
  }

  protected saveWlanRelais(): void {
    submit(this.wlanRelaisForm, async () => {
      const { wlanRelaisType, ipAddress } = this.model();
      if (!wlanRelaisType) return;

      const command: SetWlanRelaisCommand = { toolId: this.toolWlanRelais().id, wlanRelaisType, ipAddress };
      this.wlanRelaisUpdated.emit(command);
    });
  }

  protected clearWlanRelais(): void {
    const tool = this.toolWlanRelais();
    this.confirmActionDialog
      .open({
        title: 'tools.dialogs.clearWlanRelais.title',
        message: 'tools.dialogs.clearWlanRelais.message',
        entity: tool.name,
        confirmLabel: 'tools.dialogs.clearWlanRelais.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: ClearWlanRelaisCommand = { toolId: tool.id };
          this.wlanRelaisCleared.emit(command);
        }
      });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toWlanRelaisFormModel(tool: ToolWlanRelaisViewModel): WlanRelaisFormModel {
  return { wlanRelaisType: tool.wlanRelaisType ?? '', ipAddress: tool.ipAddress ?? '' };
}
