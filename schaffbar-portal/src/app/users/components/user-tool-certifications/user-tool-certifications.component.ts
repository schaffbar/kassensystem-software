import { DatePipe } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { CertificationStatusBadgeComponent } from '../../../shared/components/certification-status-badge/certification-status-badge.component';
import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../tool-certification.model';

export interface UserToolCertificationRowViewModel {
  customerId: string;
  toolId: string;
  // null if the tool has been deleted
  toolName: string | null;
  status: ToolCertificationStatus;
  certifiedAt: Date;
}

@Component({
  selector: 'schbar-user-tool-certifications',
  templateUrl: './user-tool-certifications.component.html',
  styleUrl: './user-tool-certifications.component.scss',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    CertificationStatusBadgeComponent,
    DatePipe,
    TranslatePipe,
    OrDashPipe,
  ],
})
export class UserToolCertificationsComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly certifications = input.required<UserToolCertificationRowViewModel[]>();

  readonly certificationPaused = output<PauseToolCertificationCommand>();
  readonly certificationReactivated = output<ReactivateToolCertificationCommand>();
  readonly certificationRevoked = output<RevokeToolCertificationCommand>();

  protected readonly displayedColumns: string[] = ['toolName', 'status', 'certifiedAt', 'actions'];
  protected readonly CertificationStatus = ToolCertificationStatus;

  // mat-table rows are typed as `any`; restores the type so `@default never` can check the status
  protected readonly asCertification = (row: UserToolCertificationRowViewModel): UserToolCertificationRowViewModel =>
    row;

  // --------------------------------------------------------------------------
  // actions

  protected pauseCertification(cert: UserToolCertificationRowViewModel): void {
    this.confirmActionDialog
      .open({
        title: 'users.dialogs.certifications.pauseCertification.title',
        message: 'users.dialogs.certifications.pauseCertification.message',
        entity: cert.toolName ?? undefined,
        confirmLabel: 'users.dialogs.certifications.pauseCertification.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: PauseToolCertificationCommand = { userId: cert.customerId, toolId: cert.toolId };
          this.certificationPaused.emit(command);
        }
      });
  }

  protected reactivateCertification(cert: UserToolCertificationRowViewModel): void {
    this.confirmActionDialog
      .open({
        title: 'users.dialogs.certifications.reactivateCertification.title',
        message: 'users.dialogs.certifications.reactivateCertification.message',
        entity: cert.toolName ?? undefined,
        confirmLabel: 'users.dialogs.certifications.reactivateCertification.confirm',
        destructive: false,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: ReactivateToolCertificationCommand = { userId: cert.customerId, toolId: cert.toolId };
          this.certificationReactivated.emit(command);
        }
      });
  }

  protected revokeCertification(cert: UserToolCertificationRowViewModel): void {
    this.confirmActionDialog
      .open({
        title: 'users.dialogs.certifications.revokeCertification.title',
        message: 'users.dialogs.certifications.revokeCertification.message',
        entity: cert.toolName ?? undefined,
        confirmLabel: 'users.dialogs.certifications.revokeCertification.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: RevokeToolCertificationCommand = { userId: cert.customerId, toolId: cert.toolId };
          this.certificationRevoked.emit(command);
        }
      });
  }
}
