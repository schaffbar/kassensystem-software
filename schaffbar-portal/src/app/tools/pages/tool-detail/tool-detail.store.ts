import { computed, inject } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, pipe, tap } from 'rxjs';

import { setError, setFulfilled, setPending, withRequestStatus } from '../../../shared/state/request-status.feature';
import {
  BatchCreateToolCertificationCommand,
  DeleteToolCertificationCommand,
} from '../../../users/shared/models/tool-certification.model';
import { ToolCertificationService } from '../../../users/shared/services/tool-certification.service';
import {
  AddToolInstructorsCommand,
  ChangeRfidReaderCommand,
  ClearRfidReaderCommand,
  ClearWlanRelaisCommand,
  RemoveToolInstructorsCommand,
  SetWlanRelaisCommand,
  UpdateToolCommand,
} from '../../tool.model';
import { ToolsService } from '../../tools.service';

interface ToolDetailState {
  toolId: string | null;
}

export const ToolDetailStore = signalStore(
  withState<ToolDetailState>({ toolId: null }),
  // request status of the mutations
  withRequestStatus(),
  withProps((store) => {
    const toolsService = inject(ToolsService);
    const toolCertificationService = inject(ToolCertificationService);
    return {
      _toolsService: toolsService,
      _toolCertificationService: toolCertificationService,
      // (re)load whenever the id changes – replaces the dirty flag + effect + rxMethods
      _toolResource: toolsService.toolResource(store.toolId),
      _certificationsResource: toolCertificationService.certificationsByToolResource(store.toolId),
    };
  }),
  withComputed(({ _toolResource, _certificationsResource }) => ({
    // hasValue() stays true while reloading, so the page keeps showing the data instead of flickering
    tool: computed(() => (_toolResource.hasValue() ? _toolResource.value() : null)),
    toolCertifications: computed(() => (_certificationsResource.hasValue() ? _certificationsResource.value() : [])),
    isLoading: computed(() => _toolResource.isLoading() || _certificationsResource.isLoading()),
    hasLoadError: computed(() => !!_toolResource.error() || !!_certificationsResource.error()),
  })),
  withMethods((store) => ({
    setToolId: signalMethod<string>((toolId) => {
      patchState(store, { toolId });
    }),
    reloadTool(): void {
      store._toolResource.reload();
      store._certificationsResource.reload();
    },
  })),
  // mutations of the tool – reload the tool on success
  withMethods((store) => ({
    updateTool: rxMethod<UpdateToolCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: UpdateToolCommand) => {
          return store._toolsService.updateTool(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._toolResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    changeRfidReader: rxMethod<ChangeRfidReaderCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: ChangeRfidReaderCommand) => {
          return store._toolsService.changeRfidReader(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._toolResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    clearRfidReader: rxMethod<ClearRfidReaderCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: ClearRfidReaderCommand) => {
          return store._toolsService.clearRfidReader(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._toolResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    setWlanRelais: rxMethod<SetWlanRelaisCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: SetWlanRelaisCommand) => {
          return store._toolsService.setWlanRelais(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._toolResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    clearWlanRelais: rxMethod<ClearWlanRelaisCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: ClearWlanRelaisCommand) => {
          return store._toolsService.clearWlanRelais(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._toolResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    addInstructors: rxMethod<AddToolInstructorsCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: AddToolInstructorsCommand) => {
          return store._toolsService.addInstructors(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._toolResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    removeInstructors: rxMethod<RemoveToolInstructorsCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: RemoveToolInstructorsCommand) => {
          return store._toolsService.removeInstructors(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._toolResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  // mutations of the certifications – reload the certifications on success
  withMethods((store) => ({
    batchCreateCertifications: rxMethod<BatchCreateToolCertificationCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: BatchCreateToolCertificationCommand) => {
          return store._toolCertificationService.batchCreate(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._certificationsResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    deleteCertification: rxMethod<DeleteToolCertificationCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command: DeleteToolCertificationCommand) => {
          return store._toolCertificationService.delete(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._certificationsResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
);
