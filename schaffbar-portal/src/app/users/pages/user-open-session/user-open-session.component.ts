import { Component, inject, input } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';

import { TranslatePipe } from '@ngx-translate/core';

import { ToolUsageSummaryComponent } from '../../components/tool-usage-summary/tool-usage-summary.component';
import { WorkshopUsageSummaryComponent } from '../../components/workshop-usage-summary/workshop-usage-summary.component';
import { CloseWorkshopSessionCommand } from '../../shared/models/workshop-session.model';
import { UserDetailStore } from '../../shared/stores/user-detail.store';

@Component({
  selector: 'schbar-user-open-session',
  templateUrl: './user-open-session.component.html',
  styleUrl: './user-open-session.component.scss',
  imports: [MatButtonModule, TranslatePipe, ToolUsageSummaryComponent, WorkshopUsageSummaryComponent],
})
export class UserOpenSessionComponent {
  protected readonly detailsStore = inject(UserDetailStore);

  readonly userId = input.required<string>();

  protected closeSession(): void {
    const command: CloseWorkshopSessionCommand = { customerId: this.userId() };
    this.detailsStore.closeSession(command);
  }
}
