import { Component, computed, inject } from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import {
  UserOpenSessionComponent,
  UserOpenSessionViewModel,
  UserSessionUsageRowViewModel,
} from '../../../components/user-open-session/user-open-session.component';
import { WorkshopSession } from '../../../workshop-session.model';
import { UserDetailStore } from '../user-detail.store';

@Component({
  selector: 'schbar-user-detail-tab-session',
  templateUrl: './user-detail-tab-session.component.html',
  imports: [TranslatePipe, EmptyStateComponent, UserOpenSessionComponent],
})
export class UserDetailTabSessionComponent {
  protected readonly detailsStore = inject(UserDetailStore);

  protected readonly openSession = computed(() => {
    const session = this.detailsStore.openSession();
    return session ? toUserOpenSessionViewModel(session) : null;
  });
}

// ----------------------------------------------------------------------------
// mappers

// stays in the workshop and uses of tools in the order they started
function toUserOpenSessionViewModel(session: WorkshopSession): UserOpenSessionViewModel {
  const workshopUsages: UserSessionUsageRowViewModel[] = session.workshopUsages.map((usage) => ({
    id: usage.id,
    kind: 'workshop',
    toolName: null,
    startTime: usage.entryTime,
    endTime: usage.exitTime,
    durationInMinutes: usage.durationInMinutes,
    units: usage.unitsUsed,
  }));
  const toolUsages: UserSessionUsageRowViewModel[] = session.toolUsageSummaries.flatMap((summary) =>
    summary.usages.map((usage) => ({
      id: usage.id,
      kind: 'tool',
      toolName: summary.toolName,
      startTime: usage.startTime,
      endTime: usage.endTime,
      durationInMinutes: usage.durationInMinutes,
      units: usage.unitsUsed,
    })),
  );
  return {
    usages: [...workshopUsages, ...toolUsages].sort((a, b) => a.startTime.getTime() - b.startTime.getTime()),
    workshopMinutes: session.totalWorkshopDurationInMinutes,
    workshopUnits: session.totalWorkshopUnits,
    toolUnits: session.toolUsageSummaries.reduce((sum, summary) => sum + summary.totalUnits, 0),
  };
}
