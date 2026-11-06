import { Component, computed, inject } from '@angular/core';

import {
  ToolCertificationRowViewModel,
  ToolCertificationsComponent,
} from '../../../components/tool-certifications/tool-certifications.component';
import {
  BatchCreateToolCertificationCommand,
  DeleteToolCertificationCommand,
  ToolCertification,
} from '../../../tool-certification.model';
import { User } from '../../../user.model';
import { ToolDetailStore } from '../tool-detail.store';

@Component({
  selector: 'schbar-tool-detail-tab-certifications',
  templateUrl: './tool-detail-tab-certifications.component.html',
  styleUrl: './tool-detail-tab-certifications.component.scss',
  imports: [ToolCertificationsComponent],
})
export class ToolDetailTabCertificationsComponent {
  protected readonly detailsStore = inject(ToolDetailStore);

  protected readonly certificationRows = computed(() =>
    toCertificationRowViewModels(this.detailsStore.toolCertifications(), this.detailsStore.users()),
  );

  // --------------------------------------------------------------------------
  // actions

  protected onBatchCreateCertifications(command: BatchCreateToolCertificationCommand): void {
    this.detailsStore.batchCreateCertifications(command);
  }

  protected onDeleteCertification(command: DeleteToolCertificationCommand): void {
    this.detailsStore.deleteCertification(command);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toCertificationRowViewModels(
  certifications: ToolCertification[],
  users: User[],
): ToolCertificationRowViewModel[] {
  const userNames = new Map(users.map((user) => [user.id, `${user.firstName} ${user.lastName}`]));
  return certifications.map((certification) => ({
    customerId: certification.customerId,
    toolId: certification.toolId,
    userName: userNames.get(certification.customerId) ?? null,
    status: certification.status,
    certifiedAt: certification.certifiedAt,
  }));
}
