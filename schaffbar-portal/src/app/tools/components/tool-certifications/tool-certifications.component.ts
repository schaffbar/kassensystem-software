import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { CertificationStatusBadgeComponent } from '../../../shared/components/certification-status-badge/certification-status-badge.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import {
  canPauseToolCertification,
  canReactivateToolCertification,
  canRevokeToolCertification,
  DeleteToolCertificationCommand,
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../tool-certification.model';

export interface ToolCertificationRowViewModel {
  customerId: string;
  toolId: string;
  // null if the user has been deleted
  userName: string | null;
  // null if unknown or the instructor has been deleted
  certifierName: string | null;
  status: ToolCertificationStatus;
  certifiedAt: Date;
}

@Component({
  selector: 'schbar-tool-certifications',
  templateUrl: './tool-certifications.component.html',
  styleUrl: './tool-certifications.component.scss',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatTableModule,
    TranslatePipe,
    OrDashPipe,
    CertificationStatusBadgeComponent,
    EmptyStateComponent,
    RowMenuComponent,
  ],
  host: { class: 'table-card' },
})
export class ToolCertificationsComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  readonly certifications = input.required<ToolCertificationRowViewModel[]>();

  readonly createRequested = output<void>();
  readonly certificationPaused = output<PauseToolCertificationCommand>();
  readonly certificationReactivated = output<ReactivateToolCertificationCommand>();
  readonly certificationRevoked = output<RevokeToolCertificationCommand>();
  readonly certificationDeleted = output<DeleteToolCertificationCommand>();

  protected readonly displayedColumns = ['person', 'certifiedAt', 'certifiedBy', 'status', 'actions'];
  protected readonly usersLink = `/${ROUTE.USERS}`;

  protected readonly searchText = signal('');

  protected readonly rows = computed(() => {
    const searchText = this.searchText().trim().toLowerCase();
    return this.certifications()
      .filter((certification) => !searchText || (certification.userName ?? '').toLowerCase().includes(searchText))
      .map((certification) => ({
        ...certification,
        canPause: canPauseToolCertification(certification.status),
        canReactivate: canReactivateToolCertification(certification.status),
        canRevoke: canRevokeToolCertification(certification.status),
      }))
      .sort((a, b) => (a.userName ?? '').localeCompare(b.userName ?? '', 'de'));
  });

  // --------------------------------------------------------------------------
  // actions

  protected searchCertifications(searchText: string): void {
    this.searchText.set(searchText);
  }

  protected clearSearch(): void {
    this.searchText.set('');
  }

  protected pauseToolCertification(certification: ToolCertificationRowViewModel): void {
    const command: PauseToolCertificationCommand = toCommand(certification);
    this.confirm(certification, 'pauseCertification', true, () => this.certificationPaused.emit(command));
  }

  protected reactivateToolCertification(certification: ToolCertificationRowViewModel): void {
    const command: ReactivateToolCertificationCommand = toCommand(certification);
    this.confirm(certification, 'reactivateCertification', false, () => this.certificationReactivated.emit(command));
  }

  protected revokeToolCertification(certification: ToolCertificationRowViewModel): void {
    const command: RevokeToolCertificationCommand = toCommand(certification);
    this.confirm(certification, 'revokeCertification', true, () => this.certificationRevoked.emit(command));
  }

  protected deleteToolCertification(certification: ToolCertificationRowViewModel): void {
    this.confirmDeleteDialog
      .open({
        title: 'tools.dialogs.deleteCertification.title',
        message: 'tools.dialogs.deleteCertification.message',
        confirmLabel: 'tools.dialogs.deleteCertification.confirm',
        entity: certification.userName ?? undefined,
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
    certification: ToolCertificationRowViewModel,
    dialog: 'pauseCertification' | 'reactivateCertification' | 'revokeCertification',
    destructive: boolean,
    onConfirmed: () => void,
  ): void {
    this.confirmActionDialog
      .open({
        title: `tools.dialogs.${dialog}.title`,
        message: `tools.dialogs.${dialog}.message`,
        confirmLabel: `tools.dialogs.${dialog}.confirm`,
        entity: certification.userName ?? undefined,
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
function toCommand(certification: ToolCertificationRowViewModel): { userId: string; toolId: string } {
  return { userId: certification.customerId, toolId: certification.toolId };
}
