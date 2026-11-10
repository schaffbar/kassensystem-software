import { Component, computed, inject } from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import {
  UserOpenSessionComponent,
  UserOpenSessionViewModel,
  UserSessionToolTotalViewModel,
  UserSessionToolUsageViewModel,
} from '../../../components/user-open-session/user-open-session.component';
import { ToolUsage, ToolUsageSummary, WorkshopSession, WorkshopUsage } from '../../../workshop-session.model';
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

// the stays in the workshop as a timeline, each with the tools used during it; the totals per tool for the summary
function toUserOpenSessionViewModel(session: WorkshopSession): UserOpenSessionViewModel {
  const stays = [...session.workshopUsages].sort(byStart((usage) => usage.entryTime));
  const toolUsages = session.toolUsageSummaries
    .flatMap((summary) => summary.usages.map((usage) => toUserSessionToolUsageViewModel(summary, usage)))
    .sort(byStart((usage) => usage.startTime));
  const toolUsagesByStay = new Map(stays.map((stay) => [stay.id, [] as UserSessionToolUsageViewModel[]]));
  for (const toolUsage of toolUsages) {
    const stay = stayOf(stays, toolUsage.startTime);
    if (stay) toolUsagesByStay.get(stay.id)!.push(toolUsage);
  }
  return {
    stays: stays.map((stay) => ({
      id: stay.id,
      startTime: stay.entryTime,
      endTime: stay.exitTime,
      durationInMinutes: stay.durationInMinutes,
      units: stay.unitsUsed,
      toolUsages: toolUsagesByStay.get(stay.id)!,
    })),
    workshopMinutes: session.totalWorkshopDurationInMinutes,
    workshopUnits: session.totalWorkshopUnits,
    toolUnits: session.toolUsageSummaries.reduce((sum, summary) => sum + summary.totalUnits, 0),
    tools: session.toolUsageSummaries
      .map(toUserSessionToolTotalViewModel)
      .sort((a, b) => (a.toolName ?? '').localeCompare(b.toolName ?? '', 'de')),
  };
}

function toUserSessionToolUsageViewModel(summary: ToolUsageSummary, usage: ToolUsage): UserSessionToolUsageViewModel {
  return {
    id: usage.id,
    toolName: summary.toolName,
    startTime: usage.startTime,
    endTime: usage.endTime,
    durationInMinutes: usage.durationInMinutes,
    units: usage.unitsUsed,
  };
}

function toUserSessionToolTotalViewModel(summary: ToolUsageSummary): UserSessionToolTotalViewModel {
  return {
    toolId: summary.toolId,
    toolName: summary.toolName,
    usages: summary.usages.length,
    minutes: summary.totalDurationInMinutes,
    units: summary.totalUnits,
  };
}

// ----------------------------------------------------------------------------
// helper

function byStart<T>(start: (item: T) => Date): (a: T, b: T) => number {
  return (a, b) => start(a).getTime() - start(b).getTime();
}

// the stay during which the tool was switched on (a running stay lasts until now); with inconsistent times the last
// stay that began before – or the first one – so that no usage gets lost
function stayOf(stays: WorkshopUsage[], time: Date): WorkshopUsage | undefined {
  return (
    stays.find((stay) => stay.entryTime <= time && (!stay.exitTime || time <= stay.exitTime)) ??
    [...stays].reverse().find((stay) => stay.entryTime <= time) ??
    stays[0]
  );
}
