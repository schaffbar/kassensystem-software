import { Component, input } from '@angular/core';

import { MatIconModule } from '@angular/material/icon';

// success: free, active · info: in use · warn: hint, paused · danger: required, revoked, error · neutral: offline
export type StatusBadgeTone = 'success' | 'info' | 'warn' | 'danger' | 'neutral';

@Component({
  selector: 'schbar-status-badge',
  templateUrl: './status-badge.component.html',
  styleUrl: './status-badge.component.scss',
  imports: [MatIconModule],
  host: {
    '[class]': "'tone-' + tone()",
  },
})
export class StatusBadgeComponent {
  readonly tone = input.required<StatusBadgeTone>();
  // Material icon name – a status is never shown by color alone
  readonly icon = input.required<string>();
}
