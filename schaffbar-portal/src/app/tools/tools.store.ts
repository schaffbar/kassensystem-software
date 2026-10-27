import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { mutation } from '../shared/state/mutation';
import { CreateToolCommand, DeleteToolCommand } from './tool.model';
import { ToolsService } from './tools.service';

export const ToolsStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const toolsService = inject(ToolsService);
    return {
      _toolsService: toolsService,
      // loads on first use of the store – replaces withEntities + rxMethod + onInit
      _toolsResource: toolsService.toolsResource(),
    };
  }),
  withComputed(({ _toolsResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_toolsResource.hasValue() ? _toolsResource.value() : [])),
    isLoading: computed(() => _toolsResource.isLoading()),
    hasLoadError: computed(() => !!_toolsResource.error()),
  })),
  withMethods((store) => ({
    reloadTools(): void {
      store._toolsResource.reload();
    },
    createTool: mutation<CreateToolCommand>({
      request: (command) => store._toolsService.createTool(command),
      onSuccess: () => store._toolsResource.reload(),
    }),
    deleteTool: mutation<DeleteToolCommand>({
      request: (command) => store._toolsService.deleteTool(command),
      onSuccess: () => store._toolsResource.reload(),
    }),
  })),
);
