import { Component, computed, inject } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';

import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../../../../shared/dialogs/dialog-constants';
import {
  CreateToolCertificationsDialogData,
  CreateToolCertificationsFormComponent,
} from '../../../components/create-tool-certifications-form/create-tool-certifications-form.component';
import {
  ToolCertificationRowViewModel,
  ToolCertificationsComponent,
} from '../../../components/tool-certifications/tool-certifications.component';
import {
  BatchCreateToolCertificationsCommand,
  DeleteToolCertificationCommand,
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
  ToolCertification,
} from '../../../tool-certification.model';
import { User } from '../../../user.model';
import { ToolDetailStore } from '../tool-detail.store';

@Component({
  selector: 'schbar-tool-detail-tab-certifications',
  templateUrl: './tool-detail-tab-certifications.component.html',
  imports: [ToolCertificationsComponent],
})
export class ToolDetailTabCertificationsComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  private readonly dialog = inject(MatDialog);

  protected readonly certificationRows = computed(() =>
    toToolCertificationRowViewModels(this.detailsStore.toolCertifications(), this.detailsStore.users()),
  );

  // --------------------------------------------------------------------------
  // actions

  protected batchCreateToolCertifications(): void {
    const tool = this.detailsStore.tool();
    if (!tool) return;

    const dialogRef = this.dialog.open<
      CreateToolCertificationsFormComponent,
      CreateToolCertificationsDialogData,
      BatchCreateToolCertificationsCommand
    >(CreateToolCertificationsFormComponent, {
      ...FORM_DIALOG_CONFIG,
      minWidth: DIALOG_WIDTH.medium,
      data: {
        toolId: tool.id,
        toolName: tool.name,
        certifiers: this.detailsStore.certifiers(),
        candidates: this.detailsStore.certifiableUsers(),
      },
    });

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.detailsStore.batchCreateToolCertifications(command);
      }
    });
  }

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

// ----------------------------------------------------------------------------
// mappers

function toToolCertificationRowViewModels(
  certifications: ToolCertification[],
  users: User[],
): ToolCertificationRowViewModel[] {
  const userNames = new Map(users.map((user) => [user.id, `${user.firstName} ${user.lastName}`]));
  return certifications.map((certification) => ({
    customerId: certification.customerId,
    toolId: certification.toolId,
    userName: userNames.get(certification.customerId) ?? null,
    certifierName: certification.certifiedBy ? (userNames.get(certification.certifiedBy) ?? null) : null,
    status: certification.status,
    certifiedAt: certification.certifiedAt,
  }));
}
