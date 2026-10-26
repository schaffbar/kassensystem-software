import { computed, inject } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, pipe, tap } from 'rxjs';

import { setError, setFulfilled, setPending, withRequestStatus } from '../../../shared/state/request-status.feature';
import { ChangeRfidReaderTypeCommand, UpdateRfidReaderCommand } from '../../rfid-reader.model';
import { RfidReaderService } from '../../rfid-readers.service';

interface RfidReaderDetailState {
  rfidReaderId: string | null;
}

export const RfidReaderDetailStore = signalStore(
  withState<RfidReaderDetailState>({ rfidReaderId: null }),
  // request status of the mutations (update, change type)
  withRequestStatus(),
  withProps((store) => {
    const rfidReaderService = inject(RfidReaderService);
    return {
      _rfidReaderService: rfidReaderService,
      // (re)loads whenever the id changes – replaces the dirty flag + effect + rxMethod
      _rfidReaderResource: rfidReaderService.rfidReaderResource(store.rfidReaderId),
    };
  }),
  withComputed(({ _rfidReaderResource }) => ({
    // hasValue() stays true while reloading, so the page keeps showing the reader instead of flickering
    rfidReader: computed(() => (_rfidReaderResource.hasValue() ? _rfidReaderResource.value() : null)),
    isLoading: computed(() => _rfidReaderResource.isLoading()),
    hasLoadError: computed(() => !!_rfidReaderResource.error()),
  })),
  withMethods((store) => ({
    setRfidReaderId: signalMethod<string>((rfidReaderId) => {
      patchState(store, { rfidReaderId });
    }),
    reloadRfidReader(): void {
      store._rfidReaderResource.reload();
    },
    updateRfidReader: rxMethod<UpdateRfidReaderCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command) => {
          return store._rfidReaderService.updateRfidReader(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._rfidReaderResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
    changeRfidReaderType: rxMethod<ChangeRfidReaderTypeCommand>(
      pipe(
        tap(() => patchState(store, setPending())),
        exhaustMap((command) => {
          return store._rfidReaderService.changeRfidReaderType(command).pipe(
            tapResponse({
              next: () => {
                patchState(store, setFulfilled());
                store._rfidReaderResource.reload();
              },
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
);
