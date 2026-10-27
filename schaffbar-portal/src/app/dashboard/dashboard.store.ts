import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { DashboardService } from './dashboard.service';

export const DashboardStore = signalStore(
  withProps(() => {
    const dashboardService = inject(DashboardService);
    return {
      // load when the dashboard opens – replaces withEntities + rxMethods + onInit
      _activeUsersResource: dashboardService.activeUsersResource(),
      _activeToolsResource: dashboardService.activeToolsResource(),
    };
  }),
  withComputed(({ _activeUsersResource, _activeToolsResource }) => ({
    activeUsers: computed(() => (_activeUsersResource.hasValue() ? _activeUsersResource.value() : [])),
    activeTools: computed(() => (_activeToolsResource.hasValue() ? _activeToolsResource.value() : [])),
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
