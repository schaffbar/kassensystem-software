import { inject } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withProps } from '@ngrx/signals';
import { setAllEntities, withEntities } from '@ngrx/signals/entities';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, pipe, tap } from 'rxjs';

import { mutation } from '../shared/state/mutation';
import { setError, setFulfilled, setPending, withRequestStatus } from '../shared/state/request-status.feature';
import { DeleteRfidTagCommand, RfidTag } from './rfid-tag.model';
import { RfidTagService } from './rfid-tags.service';

export const RfidTagStore = signalStore(
  { providedIn: 'root' },
  withEntities<RfidTag>(),
  withRequestStatus(),
  withProps(() => ({
    _rfidTagService: inject(RfidTagService),
  })),
  withMethods((store) => ({
    loadAllRfidTags: rxMethod<void>(
      pipe(
        tap(() => patchState(store, setPending())),
        // delay(100), // TODO: Simulate network latency
        exhaustMap(() => {
          return store._rfidTagService.getRfidTags().pipe(
            // delay(400),
            tapResponse({
              next: (rfidTags) => patchState(store, setAllEntities(rfidTags), setFulfilled()),
              error: (error: { message: string }) => patchState(store, setError(error.message)),
            }),
          );
        }),
      ),
    ),
  })),
  withMethods((store) => ({
    deleteRfidTag: mutation<DeleteRfidTagCommand>({
      request: (command) => store._rfidTagService.deleteRfidTag(command),
      onSuccess: () => store.loadAllRfidTags(),
    }),
  })),
  withHooks((store) => ({
    onInit() {
      store.loadAllRfidTags();
    },
  })),
);
