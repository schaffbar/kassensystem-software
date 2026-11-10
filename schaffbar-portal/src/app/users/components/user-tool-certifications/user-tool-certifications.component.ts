import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';

import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { CertificationRequirementBadgeComponent } from '../../../shared/components/certification-requirement-badge/certification-requirement-badge.component';
import { CertificationStatusBadgeComponent } from '../../../shared/components/certification-status-badge/certification-status-badge.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { ToolAreaKeyPipe } from '../../../shared/pipes/tool-area-key.pipe';
import {
  canPauseToolCertification,
  canReactivateToolCertification,
  canRevokeToolCertification,
  DeleteToolCertificationCommand,
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../tool-certification.model';

export interface UserToolCertificationRowViewModel {
  customerId: string;
  toolId: string;
  // tool fields are null if the tool has been deleted
  toolName: string | null;
  area: ToolArea | null;
  certificationRequirement: CertificationRequirement | null;
  // null if unknown or the instructor has been deleted
  certifierName: string | null;
  status: ToolCertificationStatus;
  certifiedAt: Date;
}

// card: a compact list on the overview tab · table: all columns on the certifications tab
export type UserToolCertificationsVariant = 'card' | 'table';

@Component({
  selector: 'schbar-user-tool-certifications',
  templateUrl: './user-tool-certifications.component.html',
  styleUrl: './user-tool-certifications.component.scss',
  imports: [
    DatePipe,
    NgTemplateOutlet,
    MatDividerModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
    TranslatePipe,
    OrDashPipe,
    ToolAreaKeyPipe,
    CertificationRequirementBadgeComponent,
    CertificationStatusBadgeComponent,
    EmptyStateComponent,
    RowMenuComponent,
  ],
  host: { class: 'table-card' },
})
export class UserToolCertificationsComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  readonly certifications = input.required<UserToolCertificationRowViewModel[]>();
  readonly variant = input<UserToolCertificationsVariant>('table');

  readonly certificationPaused = output<PauseToolCertificationCommand>();
  readonly certificationReactivated = output<ReactivateToolCertificationCommand>();
  readonly certificationRevoked = output<RevokeToolCertificationCommand>();
  readonly certificationDeleted = output<DeleteToolCertificationCommand>();

  protected readonly displayedColumns = ['tool', 'requirement', 'certifiedAt', 'certifiedBy', 'status', 'actions'];

  protected readonly rows = computed(() =>
    this.certifications()
      .map((certification) => ({
        ...certification,
        canPause: canPauseToolCertification(certification.status),
        canReactivate: canReactivateToolCertification(certification.status),
        canRevoke: canRevokeToolCertification(certification.status),
      }))
      .sort((a, b) => (a.toolName ?? '').localeCompare(b.toolName ?? '', 'de')),
  );

  // "3 aktiv · 1 pausiert" – only the statuses that occur
  protected readonly statusCounts = computed(() =>
    STATUS_ORDER.map((status) => ({
      key: `users.pages.userDetails.certifications.counts.${status}`,
      count: this.certifications().filter((certification) => certification.status === status).length,
    })).filter(({ count }) => count > 0),
  );

  // --------------------------------------------------------------------------
  // actions

  protected pauseToolCertification(certification: UserToolCertificationRowViewModel): void {
    const command: PauseToolCertificationCommand = toCommand(certification);
    this.confirm(certification, 'pauseCertification', true, () => this.certificationPaused.emit(command));
  }

  protected reactivateToolCertification(certification: UserToolCertificationRowViewModel): void {
    const command: ReactivateToolCertificationCommand = toCommand(certification);
    this.confirm(certification, 'reactivateCertification', false, () => this.certificationReactivated.emit(command));
  }

  protected revokeToolCertification(certification: UserToolCertificationRowViewModel): void {
    const command: RevokeToolCertificationCommand = toCommand(certification);
    this.confirm(certification, 'revokeCertification', true, () => this.certificationRevoked.emit(command));
  }

  protected deleteToolCertification(certification: UserToolCertificationRowViewModel): void {
    this.confirmDeleteDialog
      .open({
        title: 'users.dialogs.certifications.deleteCertification.title',
        message: 'users.dialogs.certifications.deleteCertification.message',
        confirmLabel: 'users.dialogs.certifications.deleteCertification.confirm',
        entity: certification.toolName ?? undefined,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteToolCertificationCommand = toCommand(certification);
          this.certificationDeleted.emit(command);
        }
      });
  }

  // --------------------------------------------------------------------------
  // helper

  private confirm(
    certification: UserToolCertificationRowViewModel,
    dialog: 'pauseCertification' | 'reactivateCertification' | 'revokeCertification',
    destructive: boolean,
    onConfirmed: () => void,
  ): void {
    this.confirmActionDialog
      .open({
        title: `users.dialogs.certifications.${dialog}.title`,
        message: `users.dialogs.certifications.${dialog}.message`,
        confirmLabel: `users.dialogs.certifications.${dialog}.confirm`,
        entity: certification.toolName ?? undefined,
        destructive,
      })
      .subscribe((confirmed) => {
        if (confirmed) onConfirmed();
      });
  }
}

// ----------------------------------------------------------------------------
// mappers

// all commands of a certification name it by the user and the tool
function toCommand(certification: UserToolCertificationRowViewModel): { userId: string; toolId: string } {
  return { userId: certification.customerId, toolId: certification.toolId };
}

// ----------------------------------------------------------------------------
// helper

const STATUS_ORDER = [ToolCertificationStatus.Active, ToolCertificationStatus.Paused, ToolCertificationStatus.Revoked];
