import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { CertificationStatusChipComponent } from '../../../shared/components/certification-status-chip/certification-status-chip.component';
import { ConfirmDeleteComponent } from '../../../shared/components/confirm-delete/confirm-delete.component';
import { Tool } from '../../../tools/tool.model';
import { ToolCertification, ToolCertificationStatus } from '../../shared/models/tool-certification.model';

@Component({
  selector: 'schbar-user-tool-certifications',
  templateUrl: './user-tool-certifications.component.html',
  styleUrl: './user-tool-certifications.component.scss',
  imports: [MatTableModule, MatButtonModule, MatIconModule, CertificationStatusChipComponent, DatePipe, TranslatePipe],
})
export class UserToolCertificationsComponent {
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly certifications = input.required<ToolCertification[]>();
  readonly allTools = input.required<Tool[]>();

  readonly pauseCertification = output<{ customerId: string; toolId: string }>();
  readonly reactivateCertification = output<{ customerId: string; toolId: string }>();
  readonly revokeCertification = output<{ customerId: string; toolId: string }>();

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
    const dialogRef = this.dialog.open(ConfirmDeleteComponent, {
      data: {
        title: this.translate.instant('users.dialogs.certifications.pauseCertification.title'),
        message: this.translate.instant('users.dialogs.certifications.pauseCertification.message'),
        entity: this.toolNameMap().get(cert.toolId),
        confirmLabel: this.translate.instant('users.dialogs.certifications.pauseCertification.confirm'),
      },
      minWidth: '600px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.pauseCertification.emit({ customerId: cert.customerId, toolId: cert.toolId });
      }
    });
  }

  protected onReactivate(cert: ToolCertification): void {
    const dialogRef = this.dialog.open(ConfirmDeleteComponent, {
      data: {
        title: this.translate.instant('users.dialogs.certifications.reactivateCertification.title'),
        message: this.translate.instant('users.dialogs.certifications.reactivateCertification.message'),
        entity: this.toolNameMap().get(cert.toolId),
        confirmLabel: this.translate.instant('users.dialogs.certifications.reactivateCertification.confirm'),
      },
      minWidth: '600px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.reactivateCertification.emit({ customerId: cert.customerId, toolId: cert.toolId });
      }
    });
  }

  protected onRevoke(cert: ToolCertification): void {
    const dialogRef = this.dialog.open(ConfirmDeleteComponent, {
      data: {
        title: this.translate.instant('users.dialogs.certifications.revokeCertification.title'),
        message: this.translate.instant('users.dialogs.certifications.revokeCertification.message'),
        entity: this.toolNameMap().get(cert.toolId),
        confirmLabel: this.translate.instant('users.dialogs.certifications.revokeCertification.confirm'),
      },
      minWidth: '600px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.revokeCertification.emit({ customerId: cert.customerId, toolId: cert.toolId });
      }
    });
  }
}
