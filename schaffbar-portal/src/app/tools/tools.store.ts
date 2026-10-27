import { inject } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withProps } from '@ngrx/signals';
import { setAllEntities, withEntities } from '@ngrx/signals/entities';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, pipe, tap } from 'rxjs';

import { mutation } from '../shared/state/mutation';
import { setError, setFulfilled, setPending, withRequestStatus } from '../shared/state/request-status.feature';
import { CreateToolCommand, DeleteToolCommand, Tool } from './tool.model';
import { ToolsService } from './tools.service';

export const ToolsStore = signalStore(
  { providedIn: 'root' },
  withEntities<Tool>(),
  withRequestStatus(),
  withProps(() => ({
    _toolsService: inject(ToolsService),
  })),
  withMethods((store) => ({
    loadAllTools: rxMethod<void>(
      pipe(
        tap(() => patchState(store, setPending())),
        // delay(100), // TODO: Simulate network latency
        exhaustMap(() => {
          return store._toolsService.getTools().pipe(
            // delay(400),
            tapResponse({
              next: (tools) => patchState(store, setAllEntities(tools), setFulfilled()),
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  withMethods((store) => ({
    createTool: mutation<CreateToolCommand>({
      request: (command) => store._toolsService.createTool(command),
      onSuccess: () => store.loadAllTools(),
    }),
    deleteTool: mutation<DeleteToolCommand>({
      request: (command) => store._toolsService.deleteTool(command),
      onSuccess: () => store.loadAllTools(),
    }),
  })),
  withHooks((store) => ({
    onInit() {
      store.loadAllTools();
    },
  })),
);
