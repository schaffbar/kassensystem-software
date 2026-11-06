import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { MatProgressBarModule } from '@angular/material/progress-bar';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import {
  ActiveToolsWidgetComponent,
  ToolStatusRowViewModel,
} from '../../components/active-tools-widget/active-tools-widget.component';
import { ActiveUsersWidgetComponent } from '../../components/active-users-widget/active-users-widget.component';
import { ActiveTool, Tool } from '../../dashboard.model';
import { DashboardStore } from '../../dashboard.store';

@Component({
  selector: 'schbar-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  imports: [ActiveUsersWidgetComponent, ActiveToolsWidgetComponent, MatProgressBarModule, TranslatePipe],
  providers: [DashboardStore],
})
export class DashboardComponent {
  protected readonly store = inject(DashboardStore);
  private readonly router = inject(Router);

  protected readonly toolStatuses = computed(() =>
    toToolStatusRowViewModels(this.store.tools(), this.store.activeTools()),
  );

  // --------------------------------------------------------------------------
  // actions

  protected reloadActiveUsers(): void {
    this.store.reloadActiveUsers();
  }

  protected reloadActiveTools(): void {
    this.store.reloadActiveTools();
  }

  protected openUserSession(customerId: string): void {
    this.router.navigate([ROUTE.USERS, customerId], { queryParams: { tab: 'open-session' } });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toToolStatusRowViewModels(tools: Tool[], activeTools: ActiveTool[]): ToolStatusRowViewModel[] {
  const activeToolsById = new Map(activeTools.map((activeTool) => [activeTool.toolId, activeTool]));
  return tools.map((tool) => {
    const activeTool = activeToolsById.get(tool.id);
    return {
      name: tool.name,
      usedBy: activeTool ? { firstName: activeTool.firstName, lastName: activeTool.lastName } : null,
    };
  });
}
