import { Component, computed, inject, output } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';

import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../../../../shared/dialogs/dialog-constants';
import {
  AddToolInstructorsDialogData,
  AddToolInstructorsFormComponent,
} from '../../../components/add-tool-instructors-form/add-tool-instructors-form.component';
import { ToolDetailInfoComponent } from '../../../components/tool-detail-info/tool-detail-info.component';
import {
  ToolHardwareSummaryComponent,
  ToolHardwareSummaryViewModel,
} from '../../../components/tool-hardware-summary/tool-hardware-summary.component';
import { ToolInstructorsComponent } from '../../../components/tool-instructors/tool-instructors.component';
import { openUpdateToolForm } from '../open-update-tool-form';
import { AddToolInstructorsCommand, RemoveToolInstructorsCommand, Tool } from '../../../tool.model';
import { ToolsStore } from '../../../tools.store';
import { ToolDetailStore } from '../tool-detail.store';

@Component({
  selector: 'schbar-tool-detail-tab-info',
  templateUrl: './tool-detail-tab-info.component.html',
  styleUrl: './tool-detail-tab-info.component.scss',
  imports: [ToolDetailInfoComponent, ToolInstructorsComponent, ToolHardwareSummaryComponent],
})
export class ToolDetailTabInfoComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  private readonly toolsStore = inject(ToolsStore);
  private readonly dialog = inject(MatDialog);

  // the hardware is configured in its own tab, which the page switches to
  readonly hardwareConfigurationRequested = output<void>();

  protected readonly toolDetailInfo = computed(() => {
    const tool = this.detailsStore.tool();
    return tool ? { ...tool, activeCertificationsCount: this.detailsStore.activeCertificationsCount() } : null;
  });

  protected readonly toolHardwareSummary = computed(() => {
    const tool = this.detailsStore.tool();
    return tool ? toToolHardwareSummaryViewModel(tool, this.detailsStore.assignedRfidReader()) : null;
  });

  // --------------------------------------------------------------------------
  // actions

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

  protected addToolInstructors(): void {
    const tool = this.detailsStore.tool();
    if (!tool) return;

    const dialogRef = this.dialog.open<
      AddToolInstructorsFormComponent,
      AddToolInstructorsDialogData,
      AddToolInstructorsCommand
    >(AddToolInstructorsFormComponent, {
      ...FORM_DIALOG_CONFIG,
      minWidth: DIALOG_WIDTH.medium,
      data: { toolId: tool.id, toolName: tool.name, candidates: this.detailsStore.instructorCandidates() },
    });

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.detailsStore.addToolInstructors(command);
      }
    });
  }

  protected removeToolInstructors(command: RemoveToolInstructorsCommand): void {
    this.detailsStore.removeToolInstructors(command);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toToolHardwareSummaryViewModel(
  tool: Tool,
  rfidReader: { id: string; name: string | null; macAddress: string } | null,
): ToolHardwareSummaryViewModel {
  return {
    rfidReader: rfidReader ? { id: rfidReader.id, name: rfidReader.name, macAddress: rfidReader.macAddress } : null,
    wlanRelaisType: tool.wlanRelaisType,
    ipAddress: tool.ipAddress,
  };
}
