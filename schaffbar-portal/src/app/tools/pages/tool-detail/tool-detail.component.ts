import { Component, computed, inject, input, signal } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { CertificationRequirementBadgeComponent } from '../../../shared/components/certification-requirement-badge/certification-requirement-badge.component';
import { openUpdateToolForm } from './open-update-tool-form';
import { DeleteToolCommand } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolAreaKeyPipe } from '../../../shared/pipes/tool-area-key.pipe';
import { ToolDetailTabCertificationsComponent } from './tool-detail-tab-certifications/tool-detail-tab-certifications.component';
import { ToolDetailTabConfigurationComponent } from './tool-detail-tab-configuration/tool-detail-tab-configuration.component';
import { ToolDetailTabInfoComponent } from './tool-detail-tab-info/tool-detail-tab-info.component';
import { ToolDetailStore } from './tool-detail.store';

const HARDWARE_TAB = 2;

@Component({
  selector: 'schbar-tool-detail',
  templateUrl: './tool-detail.component.html',
  styleUrl: './tool-detail.component.scss',
  imports: [
    MatTabsModule,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    TranslatePipe,
    PageHeaderComponent,
    RowMenuComponent,
    StatusBadgeComponent,
    CertificationRequirementBadgeComponent,
    ToolAreaKeyPipe,
    ToolDetailTabInfoComponent,
    ToolDetailTabCertificationsComponent,
    ToolDetailTabConfigurationComponent,
  ],
  providers: [ToolDetailStore],
})
export class ToolDetailComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  private readonly toolsStore = inject(ToolsStore);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  readonly id = input.required<string>();

  protected readonly toolsLink = `/${ROUTE.TOOLS}`;
  protected readonly selectedTab = signal(0);

  // the person using the tool right now – the tool list knows it from the active tools
  protected readonly usedBy = computed(
    () => this.toolsStore.tools().find((tool) => tool.id === this.detailsStore.tool()?.id)?.usedBy ?? null,
  );

  constructor() {
    this.detailsStore.setToolId(this.id);
    this.toolsStore.reloadTools();
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadTool(): void {
    this.detailsStore.reloadTool();
    this.toolsStore.reloadTools();
  }

  protected showHardware(): void {
    this.selectedTab.set(HARDWARE_TAB);
  }

  protected editTool(): void {
    const tool = this.detailsStore.tool();
    if (!tool) return;

    openUpdateToolForm(this.dialog, {
      tool,
      activeCertificationsCount: this.detailsStore.activeCertificationsCount(),
      existingTools: this.toolsStore.tools,
    }).subscribe((command) => {
      if (command) {
        this.detailsStore.updateTool(command);
      }
    });
  }

  protected deleteTool(): void {
    const tool = this.detailsStore.tool();
    if (!tool) return;

    this.confirmDeleteDialog
      .open({
        title: 'tools.dialogs.deleteTool.title',
        entity: tool.name,
        message: 'tools.dialogs.deleteTool.message',
        confirmLabel: 'tools.dialogs.deleteTool.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteToolCommand = { toolId: tool.id };
          this.detailsStore.deleteTool(command);
        }
      });
  }
}
