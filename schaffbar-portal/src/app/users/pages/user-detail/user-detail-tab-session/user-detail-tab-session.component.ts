import { Component, inject, input } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { CloseSessionDialogService } from '../../../../shared/dialogs/close-session-dialog/close-session-dialog.service';
import { ToolUsageSummaryComponent } from '../../../components/tool-usage-summary/tool-usage-summary.component';
import { WorkshopUsageSummaryComponent } from '../../../components/workshop-usage-summary/workshop-usage-summary.component';
import { CloseWorkshopSessionCommand } from '../../../workshop-session.model';
import { UserDetailStore } from '../user-detail.store';

@Component({
  selector: 'schbar-user-detail-tab-session',
  templateUrl: './user-detail-tab-session.component.html',
  styleUrl: './user-detail-tab-session.component.scss',
  imports: [MatButtonModule, MatIconModule, TranslatePipe, ToolUsageSummaryComponent, WorkshopUsageSummaryComponent],
})
export class UserDetailTabSessionComponent {
  protected readonly detailsStore = inject(UserDetailStore);
  private readonly closeSessionDialog = inject(CloseSessionDialogService);

  readonly userId = input.required<string>();

  // --------------------------------------------------------------------------
  // actions

  protected closeSession(): void {
    this.closeSessionDialog.open(this.userId()).subscribe((confirmed) => {
      if (confirmed) {
        const command: CloseWorkshopSessionCommand = { userId: this.userId() };
        this.detailsStore.closeSession(command);
      } else {
        // the dialog loaded the session itself and may have shown a newer state than the page
        this.detailsStore.reloadOpenSession();
      }
    });
  }
}
