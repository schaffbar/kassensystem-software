import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SegmentViewModel } from '../../../shared/components/segmented-control/segmented-control.component';
import { ToolArea } from '../../../shared/models/tool-area';
import {
  ActiveUserRowViewModel,
  ActiveUsersCardComponent,
} from '../../components/active-users-card/active-users-card.component';
import {
  DashboardAttentionComponent,
  DashboardAttentionItemViewModel,
} from '../../components/dashboard-attention/dashboard-attention.component';
import { StatTileComponent } from '../../components/stat-tile/stat-tile.component';
import {
  ALL_AREAS,
  ToolStatusCardComponent,
  ToolStatusRowViewModel,
} from '../../components/tool-status-card/tool-status-card.component';
import { ActiveTool, ActiveUser, hasStayedOvernight, RfidReader, Tool, User } from '../../dashboard.model';
import { DashboardStore } from '../../dashboard.store';

// the overview stays open at the counter – it reloads itself
const POLL_INTERVAL_MS = 60_000;
const AREAS = Object.values(ToolArea);

@Component({
  selector: 'schbar-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    TranslatePipe,
    PageHeaderComponent,
    ActiveUsersCardComponent,
    DashboardAttentionComponent,
    StatTileComponent,
    ToolStatusCardComponent,
  ],
  providers: [DashboardStore],
})
export class DashboardComponent {
  protected readonly store = inject(DashboardStore);
  private readonly translate = inject(TranslateService);

  protected readonly today = new Date();

  private readonly labels = toSignal<Record<string, string>>(
    this.translate.stream(['dashboard.toolStatus.allAreas', ...AREAS.map(toAreaKey)]),
  );

  protected readonly attentionItems = computed(() =>
    toDashboardAttentionItemViewModels(this.store.rfidReadersWithoutType(), this.store.toolsWithoutHardware()),
  );

  protected readonly activeUserRows = computed(() =>
    toActiveUserRowViewModels(this.store.activeUsers(), this.store.users(), this.store.activeTools(), new Date()),
  );

  protected readonly toolStatuses = computed(() =>
    toToolStatusRowViewModels(this.store.tools(), this.store.activeTools()),
  );

  protected readonly toolsInUse = computed(() =>
    this.store
      .activeTools()
      .map((activeTool) => activeTool.toolName)
      .join(', '),
  );

  // the counts tell how many tools each area has at all
  protected readonly areaSegments = computed<SegmentViewModel[]>(() => {
    const tools = this.store.tools();
    return [
      { id: ALL_AREAS, label: this.label('dashboard.toolStatus.allAreas'), count: tools.length },
      ...AREAS.map((area) => ({
        id: area,
        label: this.label(toAreaKey(area)),
        count: tools.filter((tool) => tool.area === area).length,
      })),
    ];
  });

  constructor() {
    this.store.pollDashboard(POLL_INTERVAL_MS);
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadDashboard(): void {
    this.store.reloadDashboard();
  }

  // --------------------------------------------------------------------------
  // helper

  private label(key: string): string {
    return this.labels()?.[key] ?? '';
  }
}

// ----------------------------------------------------------------------------
// mappers

function toDashboardAttentionItemViewModels(
  rfidReadersWithoutType: RfidReader[],
  toolsWithoutHardware: Tool[],
): DashboardAttentionItemViewModel[] {
  const items: DashboardAttentionItemViewModel[] = [];
  if (rfidReadersWithoutType.length > 0) {
    items.push({
      kind: 'rfidReadersWithoutType',
      names: rfidReadersWithoutType.map((rfidReader) => rfidReader.name ?? rfidReader.macAddress),
      link: toLink(ROUTE.RFID_READERS, rfidReadersWithoutType),
    });
  }
  if (toolsWithoutHardware.length > 0) {
    items.push({
      kind: 'toolsWithoutHardware',
      names: toolsWithoutHardware.map((tool) => tool.name),
      link: toLink(ROUTE.TOOLS, toolsWithoutHardware),
    });
  }
  return items;
}

// the age group and the tool come from the users and the active tools
function toActiveUserRowViewModels(
  activeUsers: ActiveUser[],
  users: User[],
  activeTools: ActiveTool[],
  today: Date,
): ActiveUserRowViewModel[] {
  const usersById = new Map(users.map((user) => [user.id, user]));
  const toolNamesByUserId = new Map(activeTools.map((activeTool) => [activeTool.customerId, activeTool.toolName]));
  return activeUsers.map((activeUser) => {
    const user = usersById.get(activeUser.customerId);
    return {
      customerId: activeUser.customerId,
      firstName: activeUser.firstName,
      lastName: activeUser.lastName,
      entryTime: activeUser.entryTime,
      ageGroup: user?.ageGroup ?? null,
      clubMember: user?.clubMember ?? false,
      toolName: toolNamesByUserId.get(activeUser.customerId) ?? null,
      stayedOvernight: hasStayedOvernight(activeUser, today),
    };
  });
}

function toToolStatusRowViewModels(tools: Tool[], activeTools: ActiveTool[]): ToolStatusRowViewModel[] {
  const activeToolsById = new Map(activeTools.map((activeTool) => [activeTool.toolId, activeTool]));
  return tools.map((tool) => {
    const activeTool = activeToolsById.get(tool.id);
    return {
      toolId: tool.id,
      name: tool.name,
      area: tool.area,
      usedBy: activeTool ? `${activeTool.firstName} ${activeTool.lastName}` : null,
    };
  });
}

// ----------------------------------------------------------------------------
// helper

function toAreaKey(area: ToolArea): string {
  return `shared.toolAreas.${area}`;
}

// one entity: straight to it; several: their list
function toLink(route: string, entities: { id: string }[]): string[] {
  return entities.length === 1 ? ['/', route, entities[0].id] : ['/', route];
}
