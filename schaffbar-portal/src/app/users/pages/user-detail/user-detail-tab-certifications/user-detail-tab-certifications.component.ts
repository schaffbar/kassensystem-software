import { Component, computed, inject } from '@angular/core';

import { UserToolCertificationsComponent } from '../../../components/user-tool-certifications/user-tool-certifications.component';
import {
  DeleteToolCertificationCommand,
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../../tool-certification.model';
import { toUserToolCertificationRowViewModels } from '../user-tool-certification-rows';
import { UserDetailStore } from '../user-detail.store';

@Component({
  selector: 'schbar-user-detail-tab-certifications',
  templateUrl: './user-detail-tab-certifications.component.html',
  imports: [UserToolCertificationsComponent],
})
export class UserDetailTabCertificationsComponent {
  protected readonly detailsStore = inject(UserDetailStore);

  protected readonly certificationRows = computed(() =>
    toUserToolCertificationRowViewModels(
      this.detailsStore.toolCertifications(),
      this.detailsStore.tools(),
      this.detailsStore.userNames(),
    ),
  );

  // --------------------------------------------------------------------------
  // actions

  protected pauseToolCertification(command: PauseToolCertificationCommand): void {
    this.detailsStore.pauseToolCertification(command);
  }

  protected reactivateToolCertification(command: ReactivateToolCertificationCommand): void {
    this.detailsStore.reactivateToolCertification(command);
  }

  protected revokeToolCertification(command: RevokeToolCertificationCommand): void {
    this.detailsStore.revokeToolCertification(command);
  }

  protected deleteToolCertification(command: DeleteToolCertificationCommand): void {
    this.detailsStore.deleteToolCertification(command);
  }
}
