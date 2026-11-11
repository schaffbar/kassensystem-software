import { Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import {
  SegmentedControlComponent,
  SegmentViewModel,
} from '../../../shared/components/segmented-control/segmented-control.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ToolArea } from '../../../shared/models/tool-area';
import { ToolAreaKeyPipe } from '../../../shared/pipes/tool-area-key.pipe';

export interface ToolStatusRowViewModel {
  toolId: string;
  name: string;
  area: ToolArea;
  // the person using the tool right now; null while it is free
  usedBy: string | null;
}

// the segment of all areas; the others are the areas themselves
export const ALL_AREAS = 'ALL';

// the overview shows the first tools of the area, the tool list all of them
const SHOWN_TOOLS = 8;

@Component({
  selector: 'schbar-tool-status-card',
  templateUrl: './tool-status-card.component.html',
  styleUrl: './tool-status-card.component.scss',
  imports: [
    RouterLink,
    MatIconModule,
    TranslatePipe,
    ToolAreaKeyPipe,
    EmptyStateComponent,
    SegmentedControlComponent,
    StatusBadgeComponent,
  ],
  host: { class: 'table-card' },
})
export class ToolStatusCardComponent {
  readonly toolStatuses = input.required<ToolStatusRowViewModel[]>();
  // translated: "Alle", "Holz", … – the id is ALL_AREAS or the area
  readonly areaSegments = input.required<SegmentViewModel[]>();

  protected readonly toolsLink = `/${ROUTE.TOOLS}`;

  protected readonly area = signal<string>(ALL_AREAS);

  protected readonly inUseCount = computed(() => this.toolStatuses().filter((tool) => tool.usedBy).length);

  // tools in use first, then by name
  protected readonly shownTools = computed(() => {
    const area = this.area();
    return this.toolStatuses()
      .filter((tool) => area === ALL_AREAS || tool.area === area)
      .sort((a, b) => Number(!!b.usedBy) - Number(!!a.usedBy) || a.name.localeCompare(b.name, 'de'))
      .slice(0, SHOWN_TOOLS);
  });
}
