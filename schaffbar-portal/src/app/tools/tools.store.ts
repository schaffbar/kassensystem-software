import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { ToolsApiService } from '../shared/api/tools/tools.api-service';
import { mutation } from '../shared/state/mutation';
import { toCreateToolRequest, toTool } from './tool.mapper';
import { CreateToolCommand, DeleteToolCommand } from './tool.model';

export const ToolsStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const toolsApi = inject(ToolsApiService);
    return {
      _toolsApi: toolsApi,
      // loads on first use of the store – replaces withEntities + rxMethod + onInit
      _toolsResource: toolsApi.toolsResource(),
    };
  }),
  withComputed(({ _toolsResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_toolsResource.hasValue() ? _toolsResource.value().map(toTool) : [])),
    isLoading: computed(() => _toolsResource.isLoading()),
    hasLoadError: computed(() => !!_toolsResource.error()),
  })),
  withMethods((store) => ({
    reloadTools(): void {
      store._toolsResource.reload();
    },
    createTool: mutation<CreateToolCommand>({
      request: (command) => store._toolsApi.createTool(toCreateToolRequest(command)),
      onSuccess: () => store._toolsResource.reload(),
    }),
    deleteTool: mutation<DeleteToolCommand>({
      request: (command) => store._toolsApi.deleteTool(command.id),
      onSuccess: () => store._toolsResource.reload(),
    }),
  })),
);
