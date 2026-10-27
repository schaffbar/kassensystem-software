import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { MatProgressBarModule } from '@angular/material/progress-bar';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { ToolsStore } from '../../../tools/tools.store';
import { ActiveToolsWidgetComponent } from '../../components/active-tools-widget/active-tools-widget.component';
import { ActiveUsersWidgetComponent } from '../../components/active-users-widget/active-users-widget.component';
import { ActiveUser } from '../../dashboard.model';
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
  protected readonly toolsStore = inject(ToolsStore);
  private readonly router = inject(Router);

  protected reloadActiveUsers(): void {
    this.store.reloadActiveUsers();
  }

  protected reloadActiveTools(): void {
    this.store.reloadActiveTools();
    this.toolsStore.reloadTools();
  }

  protected openUserSession(user: ActiveUser): void {
    this.router.navigate([ROUTE.USERS, user.customerId], { queryParams: { tab: 'open-session' } });
  }
}
