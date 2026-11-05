import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { MatProgressBarModule } from '@angular/material/progress-bar';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import {
  ActiveToolsWidgetComponent,
  ToolStatusViewModel,
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
    toToolStatusViewModels(this.store.tools(), this.store.activeTools()),
  );

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

function toToolStatusViewModels(tools: Tool[], activeTools: ActiveTool[]): ToolStatusViewModel[] {
  const activeToolsById = new Map(activeTools.map((activeTool) => [activeTool.toolId, activeTool]));
  return tools.map((tool) => {
    const activeTool = activeToolsById.get(tool.id);
    return {
      id: tool.id,
      name: tool.name,
      usedBy: activeTool ? { firstName: activeTool.firstName, lastName: activeTool.lastName } : null,
    };
  });
}
