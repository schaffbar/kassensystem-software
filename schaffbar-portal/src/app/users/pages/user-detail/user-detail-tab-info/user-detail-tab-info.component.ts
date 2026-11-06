import { Component, computed, inject } from '@angular/core';

import { UserAddressComponent } from '../../../components/user-address/user-address.component';
import { UserContactComponent } from '../../../components/user-contact/user-contact.component';
import {
  UserToolCertificationRowViewModel,
  UserToolCertificationsComponent,
} from '../../../components/user-tool-certifications/user-tool-certifications.component';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
  ToolCertification,
} from '../../../tool-certification.model';
import { Tool } from '../../../tool.model';
import { UpdateUserAddressCommand, UpdateUserContactCommand } from '../../../user.model';
import { UserDetailStore } from '../user-detail.store';

@Component({
  selector: 'schbar-user-detail-tab-info',
  templateUrl: './user-detail-tab-info.component.html',
  styleUrl: './user-detail-tab-info.component.scss',
  imports: [UserContactComponent, UserAddressComponent, UserToolCertificationsComponent],
})
export class UserDetailTabInfoComponent {
  protected readonly detailsStore = inject(UserDetailStore);

  protected readonly certificationRows = computed(() =>
    toUserToolCertificationRowViewModels(this.detailsStore.toolCertifications(), this.detailsStore.tools()),
  );

  // --------------------------------------------------------------------------
  // actions

  protected updateUserContact(command: UpdateUserContactCommand): void {
    this.detailsStore.updateUserContact(command);
  }

  protected updateUserAddress(command: UpdateUserAddressCommand): void {
    this.detailsStore.updateUserAddress(command);
  }

  protected pauseCertification(command: PauseToolCertificationCommand): void {
    this.detailsStore.pauseCertification(command);
  }

  protected reactivateCertification(command: ReactivateToolCertificationCommand): void {
    this.detailsStore.reactivateCertification(command);
  }

  protected revokeCertification(command: RevokeToolCertificationCommand): void {
    this.detailsStore.revokeCertification(command);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toUserToolCertificationRowViewModels(
  certifications: ToolCertification[],
  tools: Tool[],
): UserToolCertificationRowViewModel[] {
  const toolNames = new Map(tools.map((tool) => [tool.id, tool.name]));
  return certifications.map((certification) => ({
    customerId: certification.customerId,
    toolId: certification.toolId,
    toolName: toolNames.get(certification.toolId) ?? null,
    status: certification.status,
    certifiedAt: certification.certifiedAt,
  }));
}
