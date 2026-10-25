import { Component, computed, input, linkedSignal, output } from '@angular/core';
import { form, FormField, FormRoot, hidden, pattern, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { SetWlanRelaisCommand, Tool, WLAN_RELAIS_TEMPLATES, WlanRelaisType } from '../../tool.model';

const IP_PATTERN = /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/;

interface WlanRelaisModel {
  // '' = no WLAN relais
  wlanRelaisType: WlanRelaisType | '';
  ipAddress: string;
}

@Component({
  selector: 'schbar-tool-wlan-relais',
  templateUrl: './tool-wlan-relais.component.html',
  styleUrl: './tool-wlan-relais.component.scss',
  imports: [FormField, FormRoot, FieldErrorComponent, MatSelectModule, MatInputModule, MatButtonModule, TranslatePipe],
})
export class ToolWlanRelaisComponent {
  readonly tool = input.required<Tool>();

  readonly wlanRelaisUpdated = output<SetWlanRelaisCommand>();
  readonly wlanRelaisCleared = output<string>();

  protected readonly wlanRelaisTypes = Object.values(WlanRelaisType);

  private readonly model = linkedSignal(() => toModel(this.tool()));

  protected readonly wlanRelaisForm = form(this.model, (schema) => {
    // a hidden field does not count for the validity of the form, so the IP rules only apply with a relais type
    hidden(schema.ipAddress, { when: ({ valueOf }) => !valueOf(schema.wlanRelaisType) });
    required(schema.ipAddress);
    pattern(schema.ipAddress, IP_PATTERN, { message: 'tools.pages.toolDetails.errors.invalidIpAddress' });
  });

  protected readonly derivedCommands = computed(() => {
    const { wlanRelaisType, ipAddress } = this.model();
    if (!wlanRelaisType) {
      return { httpStartCommand: '', onCommand: '', offCommand: '' };
    }
    const template = WLAN_RELAIS_TEMPLATES[wlanRelaisType];
    return {
      httpStartCommand: template.httpStartCommand.replace('{{ipAddress}}', ipAddress),
      onCommand: template.onCommand,
      offCommand: template.offCommand,
    };
  });

  protected readonly isDirty = computed(() => {
    const current = relevantValues(this.model());
    const saved = relevantValues(toModel(this.tool()));
    return current.wlanRelaisType !== saved.wlanRelaisType || current.ipAddress !== saved.ipAddress;
  });

  protected onCancel(): void {
    this.model.set(toModel(this.tool()));
    this.wlanRelaisForm().reset();
  }

  protected onSave(): void {
    submit(this.wlanRelaisForm, async () => {
      const { wlanRelaisType, ipAddress } = this.model();
      if (wlanRelaisType) {
        const command: SetWlanRelaisCommand = { toolId: this.tool().id, wlanRelaisType, ipAddress };
        this.wlanRelaisUpdated.emit(command);
      } else {
        this.wlanRelaisCleared.emit(this.tool().id);
      }
    });
  }
}

function toModel(tool: Tool): WlanRelaisModel {
  return { wlanRelaisType: tool.wlanRelaisType ?? '', ipAddress: tool.ipAddress ?? '' };
}

// Without a relais type the (hidden) IP address is irrelevant – it is neither saved nor should it count as a change.
// It stays in the model, so switching to '--' and back does not lose the entered IP address.
function relevantValues({ wlanRelaisType, ipAddress }: WlanRelaisModel): WlanRelaisModel {
  return wlanRelaisType ? { wlanRelaisType, ipAddress } : { wlanRelaisType: '', ipAddress: '' };
}
