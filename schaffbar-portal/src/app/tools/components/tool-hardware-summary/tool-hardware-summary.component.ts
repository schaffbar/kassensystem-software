import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { WlanRelaisTypeKeyPipe } from '../../wlan-relais-type-key.pipe';

export interface ToolHardwareSummaryViewModel {
  rfidReader: { id: string; name: string | null; macAddress: string } | null;
  wlanRelaisType: WlanRelaisType | null;
  ipAddress: string | null;
}

@Component({
  selector: 'schbar-tool-hardware-summary',
  templateUrl: './tool-hardware-summary.component.html',
  styleUrl: './tool-hardware-summary.component.scss',
  imports: [RouterLink, MatButtonModule, MatIconModule, TranslatePipe, WlanRelaisTypeKeyPipe],
  host: { class: 'card' },
})
export class ToolHardwareSummaryComponent {
  readonly toolHardwareSummary = input.required<ToolHardwareSummaryViewModel>();

  readonly configureRequested = output<void>();

  protected readonly rfidReadersLink = `/${ROUTE.RFID_READERS}`;
}
