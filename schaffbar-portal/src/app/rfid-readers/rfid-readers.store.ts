import { inject } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withProps } from '@ngrx/signals';
import { setAllEntities, withEntities } from '@ngrx/signals/entities';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, pipe, tap } from 'rxjs';

import { mutation } from '../shared/state/mutation';
import { setError, setFulfilled, setPending, withRequestStatus } from '../shared/state/request-status.feature';
import { DeleteRfidReaderCommand, RfidReader } from './rfid-reader.model';
import { RfidReaderService } from './rfid-readers.service';

export const RfidReaderStore = signalStore(
  { providedIn: 'root' },
  withEntities<RfidReader>(),
  withRequestStatus(),
  withProps(() => ({
    _rfidReaderService: inject(RfidReaderService),
  })),
  withMethods((store) => ({
    loadAllRfidReaders: rxMethod<void>(
      pipe(
        tap(() => patchState(store, setPending())),
        // delay(100), // TODO: Simulate network latency
        exhaustMap(() => {
          return store._rfidReaderService.getRfidReaders().pipe(
            // delay(400),
            tapResponse({
              next: (rfidReaders) => patchState(store, setAllEntities(rfidReaders), setFulfilled()),
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  withMethods((store) => ({
    deleteRfidReader: mutation<DeleteRfidReaderCommand>({
      request: (command) => store._rfidReaderService.deleteRfidReader(command),
      onSuccess: () => store.loadAllRfidReaders(),
    }),
  })),
  withHooks((store) => ({
    onInit() {
      store.loadAllRfidReaders();
    },
  })),
);
