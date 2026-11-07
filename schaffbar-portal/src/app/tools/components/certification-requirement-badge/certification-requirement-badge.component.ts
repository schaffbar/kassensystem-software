import { Component, computed, input } from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

import { StatusBadgeComponent, StatusBadgeTone } from '../../../shared/components/status-badge/status-badge.component';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { CertificationRequirementKeyPipe } from '../../certification-requirement-key.pipe';

@Component({
  selector: 'schbar-certification-requirement-badge',
  templateUrl: './certification-requirement-badge.component.html',
  imports: [StatusBadgeComponent, CertificationRequirementKeyPipe, TranslatePipe],
})
export class CertificationRequirementBadgeComponent {
  readonly certificationRequirement = input.required<CertificationRequirement>();

  protected readonly badge = computed(() => BADGES[this.certificationRequirement()]);
}

// ----------------------------------------------------------------------------
// helper

const BADGES: Record<CertificationRequirement, { tone: StatusBadgeTone; icon: string }> = {
  [CertificationRequirement.Yellow]: { tone: 'warn', icon: 'info' },
  [CertificationRequirement.Red]: { tone: 'danger', icon: 'shield' },
};
