import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { CertificationStatusChipComponent } from '../../../shared/components/certification-status-chip/certification-status-chip.component';
import { ConfirmDialogService } from '../../../shared/components/confirm-delete/confirm-dialog.service';
import { Tool } from '../../../tools/tool.model';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
  ToolCertification,
  ToolCertificationStatus,
} from '../../shared/models/tool-certification.model';

@Component({
  selector: 'schbar-user-tool-certifications',
  templateUrl: './user-tool-certifications.component.html',
  styleUrl: './user-tool-certifications.component.scss',
  imports: [MatTableModule, MatButtonModule, MatIconModule, CertificationStatusChipComponent, DatePipe, TranslatePipe],
})
export class UserToolCertificationsComponent {
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly certifications = input.required<ToolCertification[]>();
  readonly allTools = input.required<Tool[]>();

  readonly pauseCertification = output<PauseToolCertificationCommand>();
  readonly reactivateCertification = output<ReactivateToolCertificationCommand>();
  readonly revokeCertification = output<RevokeToolCertificationCommand>();

  protected readonly displayedColumns: string[] = ['toolName', 'status', 'certifiedAt', 'actions'];
  protected readonly CertificationStatus = ToolCertificationStatus;

  // TOOD: new after migration - need to understand this
  // mat-table rows are typed as `any`; restores the type so `@default never` can check the status
  protected readonly asCertification = (row: ToolCertification): ToolCertification => row;

  protected readonly toolNameMap = computed(() => {
    const map = new Map<string, string>();
    for (const t of this.allTools()) {
      map.set(t.id, t.name);
    }
    return map;
  });

  protected onPause(cert: ToolCertification): void {
    this.confirmDialog
      .confirm({
        title: 'users.dialogs.certifications.pauseCertification.title',
        message: 'users.dialogs.certifications.pauseCertification.message',
        entity: this.toolNameMap().get(cert.toolId),
        confirmLabel: 'users.dialogs.certifications.pauseCertification.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: PauseToolCertificationCommand = { customerId: cert.customerId, toolId: cert.toolId };
          this.pauseCertification.emit(command);
        }
      });
  }

  protected onReactivate(cert: ToolCertification): void {
    this.confirmDialog
      .confirm({
        title: 'users.dialogs.certifications.reactivateCertification.title',
        message: 'users.dialogs.certifications.reactivateCertification.message',
        entity: this.toolNameMap().get(cert.toolId),
        confirmLabel: 'users.dialogs.certifications.reactivateCertification.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: ReactivateToolCertificationCommand = { customerId: cert.customerId, toolId: cert.toolId };
          this.reactivateCertification.emit(command);
        }
      });
  }

  protected onRevoke(cert: ToolCertification): void {
    this.confirmDialog
      .confirm({
        title: 'users.dialogs.certifications.revokeCertification.title',
        message: 'users.dialogs.certifications.revokeCertification.message',
        entity: this.toolNameMap().get(cert.toolId),
        confirmLabel: 'users.dialogs.certifications.revokeCertification.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: RevokeToolCertificationCommand = { customerId: cert.customerId, toolId: cert.toolId };
          this.revokeCertification.emit(command);
        }
      });
  }
}
