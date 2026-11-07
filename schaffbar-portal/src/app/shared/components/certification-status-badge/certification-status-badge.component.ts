import { Component, computed, input } from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

import { ToolCertificationStatus } from '../../models/tool-certification-status';
import { StatusBadgeComponent, StatusBadgeTone } from '../status-badge/status-badge.component';

@Component({
  selector: 'schbar-certification-status-badge',
  templateUrl: './certification-status-badge.component.html',
  imports: [StatusBadgeComponent, TranslatePipe],
})
export class CertificationStatusBadgeComponent {
  readonly status = input.required<ToolCertificationStatus>();

  protected readonly badge = computed(() => BADGES[this.status()]);
}

// ----------------------------------------------------------------------------
// helper

const BADGES: Record<ToolCertificationStatus, { tone: StatusBadgeTone; icon: string }> = {
  [ToolCertificationStatus.Active]: { tone: 'success', icon: 'check_circle' },
  [ToolCertificationStatus.Paused]: { tone: 'warn', icon: 'pause_circle' },
  [ToolCertificationStatus.Revoked]: { tone: 'danger', icon: 'block' },
};
