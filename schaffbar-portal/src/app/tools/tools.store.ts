import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { ToolDashboardApiService } from '../shared/api/tool-dashboard/tool-dashboard.api-service';
import { ToolsApiService } from '../shared/api/tools/tools.api-service';
import { mutation } from '../shared/state/mutation';
import { toCreateToolRequest, toToolListItems } from './tool.mapper';
import { CreateToolCommand, DeleteToolCommand } from './tool.model';

export const ToolsStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const toolsApi = inject(ToolsApiService);
    return {
      _toolsApi: toolsApi,
      // both load on first use of the store; the active tools tell who uses a tool right now
      _toolsResource: toolsApi.toolsResource(),
      _activeToolsResource: inject(ToolDashboardApiService).activeToolsResource(),
    };
  }),
  withComputed(({ _toolsResource, _activeToolsResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    tools: computed(() =>
      _toolsResource.hasValue()
        ? toToolListItems(_toolsResource.value(), _activeToolsResource.hasValue() ? _activeToolsResource.value() : [])
        : [],
    ),
    isLoading: computed(() => _toolsResource.isLoading() || _activeToolsResource.isLoading()),
    hasLoadError: computed(() => !!_toolsResource.error() || !!_activeToolsResource.error()),
  })),
  withMethods((store) => ({
    reloadTools(): void {
      store._toolsResource.reload();
      store._activeToolsResource.reload();
    },
    createTool: mutation<CreateToolCommand>({
      request: (command) => store._toolsApi.createTool(toCreateToolRequest(command)),
      onSuccess: () => store._toolsResource.reload(),
    }),
    deleteTool: mutation<DeleteToolCommand>({
      request: (command) => store._toolsApi.deleteTool(command.toolId),
      onSuccess: () => store._toolsResource.reload(),
    }),
  })),
);
