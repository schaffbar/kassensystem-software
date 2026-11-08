import { Component, computed, input } from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

import { StatusBadgeComponent, StatusBadgeTone } from '../../../shared/components/status-badge/status-badge.component';
import { RfidTagState } from '../../rfid-tag.model';
import { RfidTagStateKeyPipe } from '../../rfid-tag-state-key.pipe';

@Component({
  selector: 'schbar-rfid-tag-state-badge',
  template: `
    <schbar-status-badge [tone]="badge().tone" [icon]="badge().icon">
      {{ state() | rfidTagStateKey | translate }}
    </schbar-status-badge>
  `,
  imports: [StatusBadgeComponent, TranslatePipe, RfidTagStateKeyPipe],
})
export class RfidTagStateBadgeComponent {
  readonly state = input.required<RfidTagState>();

  protected readonly badge = computed(() => BADGES[this.state()]);
}

// ----------------------------------------------------------------------------
// helper

const BADGES: Record<RfidTagState, { tone: StatusBadgeTone; icon: string }> = {
  assigned: { tone: 'success', icon: 'check_circle' },
  free: { tone: 'info', icon: 'radio_button_unchecked' },
  deactivated: { tone: 'neutral', icon: 'block' },
};
