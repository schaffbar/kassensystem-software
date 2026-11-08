import { Component, computed, inject } from '@angular/core';

import {
  RfidReaderOptionViewModel,
  ToolRfidReaderAssignmentComponent,
} from '../../../components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component';
import { ToolWlanRelaisComponent } from '../../../components/tool-wlan-relais/tool-wlan-relais.component';
import { RfidReader } from '../../../rfid-reader.model';
import {
  canClearRfidReader,
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

  protected readonly rfidReaderAssignment = computed(() => {
    const tool = this.detailsStore.tool();
    if (!tool) return null;

    // the tool list knows who uses the tool right now
    const usedBy = this.toolsStore.tools().find((listedTool) => listedTool.id === tool.id)?.usedBy ?? null;
    return { ...tool, canClearRfidReader: canClearRfidReader({ ...tool, usedBy }) };
  });

  protected readonly rfidReaderOptions = computed(() =>
    toRfidReaderOptionViewModels(
      this.detailsStore.assignableRfidReaders(),
      this.toolsStore.tools(),
      this.detailsStore.tool()?.id,
    ),
  );

  // --------------------------------------------------------------------------
  // actions

  protected changeRfidReader(command: ChangeRfidReaderCommand): void {
    this.detailsStore.changeRfidReader(command);
  }

  protected clearRfidReader(command: ClearRfidReaderCommand): void {
    this.detailsStore.clearRfidReader(command);
  }

  protected setWlanRelais(command: SetWlanRelaisCommand): void {
    this.detailsStore.setWlanRelais(command);
  }

  protected clearWlanRelais(command: ClearWlanRelaisCommand): void {
    this.detailsStore.clearWlanRelais(command);
  }
}

// ----------------------------------------------------------------------------
// mappers

// a switch box of another tool is not offered – it would have to be disconnected there first
function toRfidReaderOptionViewModels(
  rfidReaders: RfidReader[],
  tools: Tool[],
  ownToolId: string | undefined,
): RfidReaderOptionViewModel[] {
  const assignedToOtherTools = new Set(
    tools.flatMap((tool) => (tool.id !== ownToolId && tool.rfidReaderId ? [tool.rfidReaderId] : [])),
  );
  return rfidReaders
    .filter((reader) => !assignedToOtherTools.has(reader.id))
    .map((reader) => ({ id: reader.id, name: reader.name, macAddress: reader.macAddress }));
}
