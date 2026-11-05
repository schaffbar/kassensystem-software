import { DatePipe } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { CertificationStatusChipComponent } from '../../../shared/components/certification-status-chip/certification-status-chip.component';
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
    CertificationStatusChipComponent,
    DatePipe,
    TranslatePipe,
    OrDashPipe,
  ],
})
export class UserToolCertificationsComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly certifications = input.required<UserToolCertificationRowViewModel[]>();

  readonly pauseCertification = output<PauseToolCertificationCommand>();
  readonly reactivateCertification = output<ReactivateToolCertificationCommand>();
  readonly revokeCertification = output<RevokeToolCertificationCommand>();

  protected readonly displayedColumns: string[] = ['toolName', 'status', 'certifiedAt', 'actions'];
  protected readonly CertificationStatus = ToolCertificationStatus;

  // TOOD: new after migration - need to understand this
  // mat-table rows are typed as `any`; restores the type so `@default never` can check the status
  protected readonly asCertification = (row: UserToolCertificationRowViewModel): UserToolCertificationRowViewModel =>
    row;

  protected onPause(cert: UserToolCertificationRowViewModel): void {
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
          const command: PauseToolCertificationCommand = { customerId: cert.customerId, toolId: cert.toolId };
          this.pauseCertification.emit(command);
        }
      });
  }

  protected onReactivate(cert: UserToolCertificationRowViewModel): void {
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
          const command: ReactivateToolCertificationCommand = { customerId: cert.customerId, toolId: cert.toolId };
          this.reactivateCertification.emit(command);
        }
      });
  }

  protected onRevoke(cert: UserToolCertificationRowViewModel): void {
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
          const command: RevokeToolCertificationCommand = { customerId: cert.customerId, toolId: cert.toolId };
          this.revokeCertification.emit(command);
        }
      });
  }
}
