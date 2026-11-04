import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { ToolDashboardApiService } from '../shared/api/tool-dashboard/tool-dashboard.api-service';
import { WorkshopDashboardApiService } from '../shared/api/workshop-dashboard/workshop-dashboard.api-service';
import { toActiveTool, toActiveUser } from './dashboard.mapper';

export const DashboardStore = signalStore(
  withProps(() => {
    return {
      // load when the dashboard opens – replaces withEntities + rxMethods + onInit
      _activeUsersResource: inject(WorkshopDashboardApiService).activeUsersResource(),
      _activeToolsResource: inject(ToolDashboardApiService).activeToolsResource(),
    };
  }),
  withComputed(({ _activeUsersResource, _activeToolsResource }) => ({
    activeUsers: computed(() =>
      _activeUsersResource.hasValue() ? _activeUsersResource.value().map(toActiveUser) : [],
    ),
    activeTools: computed(() =>
      _activeToolsResource.hasValue() ? _activeToolsResource.value().map(toActiveTool) : [],
    ),
    isLoading: computed(() => _activeUsersResource.isLoading() || _activeToolsResource.isLoading()),
    hasLoadError: computed(() => !!_activeUsersResource.error() || !!_activeToolsResource.error()),
  })),
  withMethods((store) => ({
    reloadActiveUsers(): void {
      store._activeUsersResource.reload();
    },
    reloadActiveTools(): void {
      store._activeToolsResource.reload();
    },
  })),
);
