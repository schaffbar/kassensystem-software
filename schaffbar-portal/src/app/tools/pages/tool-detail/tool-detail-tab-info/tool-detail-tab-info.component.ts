import { Component, inject } from '@angular/core';

import { ToolDetailInfoComponent } from '../../../components/tool-detail-info/tool-detail-info.component';
import { ToolInstructorsComponent } from '../../../components/tool-instructors/tool-instructors.component';
import { AddToolInstructorsCommand, RemoveToolInstructorsCommand, UpdateToolCommand } from '../../../tool.model';
import { ToolsStore } from '../../../tools.store';
import { ToolDetailStore } from '../tool-detail.store';

@Component({
  selector: 'schbar-tool-detail-tab-info',
  templateUrl: './tool-detail-tab-info.component.html',
  styleUrl: './tool-detail-tab-info.component.scss',
  imports: [ToolDetailInfoComponent, ToolInstructorsComponent],
})
export class ToolDetailTabInfoComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  protected readonly toolsStore = inject(ToolsStore);

  // --------------------------------------------------------------------------
  // actions

  protected onToolUpdated(command: UpdateToolCommand): void {
    this.detailsStore.updateTool(command);
  }

  protected onInstructorsAdded(command: AddToolInstructorsCommand): void {
    this.detailsStore.addInstructors(command);
  }

  protected onInstructorsRemoved(command: RemoveToolInstructorsCommand): void {
    this.detailsStore.removeInstructors(command);
  }
}
