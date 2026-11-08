import { ClipboardModule } from '@angular/cdk/clipboard';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface RfidReaderDetailInfoViewModel {
  name: string | null;
  macAddress: string;
  socketName: string | null;
  createdAt: Date;
}

const COPIED_FEEDBACK_MS = 2000;

@Component({
  selector: 'schbar-rfid-reader-detail-info',
  templateUrl: './rfid-reader-detail-info.component.html',
  styleUrl: './rfid-reader-detail-info.component.scss',
  imports: [ClipboardModule, DatePipe, MatButtonModule, MatIconModule, TranslatePipe, OrDashPipe],
  host: { class: 'card' },
})
export class RfidReaderDetailInfoComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly rfidReaderDetailInfo = input.required<RfidReaderDetailInfoViewModel>();

  readonly editRequested = output<void>();

  protected readonly isMacAddressCopied = signal(false);

  // --------------------------------------------------------------------------
  // actions

  protected showMacAddressCopied(copied: boolean): void {
    if (!copied) return;

    this.isMacAddressCopied.set(true);
    const timeout = setTimeout(() => this.isMacAddressCopied.set(false), COPIED_FEEDBACK_MS);
    this.destroyRef.onDestroy(() => clearTimeout(timeout));
  }
}
