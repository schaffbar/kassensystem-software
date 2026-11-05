import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { ToolDashboardApiService } from '../shared/api/tool-dashboard/tool-dashboard.api-service';
import { ToolsApiService } from '../shared/api/tools/tools.api-service';
import { WorkshopDashboardApiService } from '../shared/api/workshop-dashboard/workshop-dashboard.api-service';
import { toActiveTool, toActiveUser, toTool } from './dashboard.mapper';

export const DashboardStore = signalStore(
  withProps(() => {
    return {
      // load when the dashboard opens – replaces withEntities + rxMethods + onInit
      _activeUsersResource: inject(WorkshopDashboardApiService).activeUsersResource(),
      _activeToolsResource: inject(ToolDashboardApiService).activeToolsResource(),
      _toolsResource: inject(ToolsApiService).toolsResource(),
    };
  }),
  withComputed(({ _activeUsersResource, _activeToolsResource, _toolsResource }) => {
    const resources = [_activeUsersResource, _activeToolsResource, _toolsResource];
    return {
      activeUsers: computed(() =>
        _activeUsersResource.hasValue() ? _activeUsersResource.value().map(toActiveUser) : [],
      ),
      activeTools: computed(() =>
        _activeToolsResource.hasValue() ? _activeToolsResource.value().map(toActiveTool) : [],
      ),
      tools: computed(() => (_toolsResource.hasValue() ? _toolsResource.value().map(toTool) : [])),
      isLoading: computed(() => resources.some((resource) => resource.isLoading())),
      hasLoadError: computed(() => resources.some((resource) => !!resource.error())),
    };
  }),
  withMethods((store) => ({
    reloadActiveUsers(): void {
      store._activeUsersResource.reload();
    },
    reloadActiveTools(): void {
      store._activeToolsResource.reload();
      store._toolsResource.reload();
    },
  })),
);
