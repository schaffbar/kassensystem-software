import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

// something that has to be set up before it works – one item per kind, naming the entities
export interface DashboardAttentionItemViewModel {
  kind: 'rfidReadersWithoutType' | 'toolsWithoutHardware';
  names: string[];
  // where to fix it: the entity itself when there is one, otherwise its list
  link: string[];
}

@Component({
  selector: 'schbar-dashboard-attention',
  templateUrl: './dashboard-attention.component.html',
  styleUrl: './dashboard-attention.component.scss',
  imports: [RouterLink, MatIconModule, TranslatePipe],
})
export class DashboardAttentionComponent {
  readonly items = input.required<DashboardAttentionItemViewModel[]>();

  protected readonly lines = computed(() =>
    this.items().map((item) => {
      const plural = item.names.length > 1 ? 'other' : 'one';
      return {
        ...item,
        messageKey: `dashboard.attention.${item.kind}.${plural}`,
        linkKey: `dashboard.attention.${item.kind}.link.${plural}`,
        params: { count: item.names.length, name: item.names[0], names: toNameList(item.names) },
      };
    }),
  );
}

// ----------------------------------------------------------------------------
// helper

const SHOWN_NAMES = 3;

function toNameList(names: string[]): string {
  const shown = names.slice(0, SHOWN_NAMES).join(', ');
  return names.length > SHOWN_NAMES ? `${shown} …` : shown;
}
