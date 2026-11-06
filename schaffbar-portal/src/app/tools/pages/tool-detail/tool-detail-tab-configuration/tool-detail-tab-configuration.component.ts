import { Component, computed, inject } from '@angular/core';

import {
  RfidReaderOptionViewModel,
  ToolRfidReaderAssignmentComponent,
} from '../../../components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component';
import { ToolWlanRelaisComponent } from '../../../components/tool-wlan-relais/tool-wlan-relais.component';
import { RfidReader } from '../../../rfid-reader.model';
import {
  ChangeRfidReaderCommand,
  ClearRfidReaderCommand,
  ClearWlanRelaisCommand,
  SetWlanRelaisCommand,
  Tool,
} from '../../../tool.model';
import { ToolsStore } from '../../../tools.store';
import { ToolDetailStore } from '../tool-detail.store';

@Component({
  selector: 'schbar-tool-detail-tab-configuration',
  templateUrl: './tool-detail-tab-configuration.component.html',
  styleUrl: './tool-detail-tab-configuration.component.scss',
  imports: [ToolRfidReaderAssignmentComponent, ToolWlanRelaisComponent],
})
export class ToolDetailTabConfigurationComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  private readonly toolsStore = inject(ToolsStore);

  protected readonly rfidReaderOptions = computed(() =>
    toRfidReaderOptionViewModels(
      this.detailsStore.assignableRfidReaders(),
      this.toolsStore.entities(),
      this.detailsStore.tool()?.id,
    ),
  );

  // --------------------------------------------------------------------------
  // actions

  protected onRfidReaderChanged(command: ChangeRfidReaderCommand): void {
    this.detailsStore.changeRfidReader(command);
  }

  protected onRfidReaderCleared(command: ClearRfidReaderCommand): void {
    this.detailsStore.clearRfidReader(command);
  }

  protected onWlanRelaisUpdated(command: SetWlanRelaisCommand): void {
    this.detailsStore.setWlanRelais(command);
  }

  protected onWlanRelaisCleared(command: ClearWlanRelaisCommand): void {
    this.detailsStore.clearWlanRelais(command);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toRfidReaderOptionViewModels(
  rfidReaders: RfidReader[],
  tools: Tool[],
  ownToolId: string | undefined,
): RfidReaderOptionViewModel[] {
  const assignedToOtherTools = new Set(
    tools.flatMap((tool) => (tool.id !== ownToolId && tool.rfidReaderId ? [tool.rfidReaderId] : [])),
  );
  return rfidReaders.map((reader) => ({
    id: reader.id,
    name: reader.name,
    isAssignedToOtherTool: assignedToOtherTools.has(reader.id),
  }));
}
