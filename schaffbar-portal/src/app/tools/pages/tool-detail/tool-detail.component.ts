import { Component, computed, inject, input } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TranslatePipe } from '@ngx-translate/core';

import { RfidReadersStore } from '../../../rfid-readers/rfid-readers.store';
import {
  BatchCreateToolCertificationCommand,
  DeleteToolCertificationCommand,
} from '../../../users/shared/models/tool-certification.model';
import { UsersStore } from '../../../users/shared/stores/users.store';
import { ToolCertificationsComponent } from '../../components/tool-certifications/tool-certifications.component';
import { ToolDetailInfoComponent } from '../../components/tool-detail-info/tool-detail-info.component';
import { ToolInstructorsComponent } from '../../components/tool-instructors/tool-instructors.component';
import { ToolRfidReaderAssignmentComponent } from '../../components/tool-rfid-reader-assignment/tool-rfid-reader-assignment.component';
import { ToolWlanRelaisComponent } from '../../components/tool-wlan-relais/tool-wlan-relais.component';
import {
  AddToolInstructorsCommand,
  ChangeRfidReaderCommand,
  ClearRfidReaderCommand,
  ClearWlanRelaisCommand,
  RemoveToolInstructorsCommand,
  SetWlanRelaisCommand,
  UpdateToolCommand,
} from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolDetailStore } from './tool-detail.store';

@Component({
  selector: 'schbar-tool-detail',
  templateUrl: './tool-detail.component.html',
  styleUrl: './tool-detail.component.scss',
  imports: [
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    TranslatePipe,
    ToolDetailInfoComponent,
    ToolInstructorsComponent,
    ToolRfidReaderAssignmentComponent,
    ToolWlanRelaisComponent,
    ToolCertificationsComponent,
  ],
  providers: [ToolDetailStore],
})
export class ToolDetailComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  private readonly rfidReadersStore = inject(RfidReadersStore);
  private readonly toolsStore = inject(ToolsStore);
  private readonly usersStore = inject(UsersStore);

  readonly id = input.required<string>();

  protected readonly selectedTool = computed(() => this.detailsStore.tool());
  protected readonly toolCertifications = computed(() => this.detailsStore.toolCertifications());
  protected readonly allUsers = computed(() => this.usersStore.entities());
  protected readonly allRfidReaders = computed(() => this.rfidReadersStore.entities());
  protected readonly assignedRfidReaderIds = computed(() => {
    const currentToolId = this.selectedTool()?.id;
    const ids = this.toolsStore
      .entities()
      .filter((tool) => tool.id !== currentToolId && tool.rfidReaderId)
      .map((tool) => tool.rfidReaderId!);
    return new Set(ids);
  });

  constructor() {
    this.detailsStore.setToolId(this.id);
    this.rfidReadersStore.reloadRfidReaders();
    this.toolsStore.reloadTools();
    this.usersStore.reloadUsers();
  }

  protected reloadTool(): void {
    this.detailsStore.reloadTool();
    this.rfidReadersStore.reloadRfidReaders();
    this.toolsStore.reloadTools();
  }

  protected onToolUpdated(command: UpdateToolCommand): void {
    this.detailsStore.updateTool(command);
  }

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

  protected onBatchCreateCertifications(command: BatchCreateToolCertificationCommand): void {
    this.detailsStore.batchCreateCertifications(command);
  }

  protected onDeleteCertification(command: DeleteToolCertificationCommand): void {
    this.detailsStore.deleteCertification(command);
  }

  protected onInstructorsAdded(command: AddToolInstructorsCommand): void {
    this.detailsStore.addInstructors(command);
  }

  protected onInstructorsRemoved(command: RemoveToolInstructorsCommand): void {
    this.detailsStore.removeInstructors(command);
  }
}
