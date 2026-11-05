import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { RfidReadersApiService } from '../shared/api/rfid-readers/rfid-readers.api-service';
import { mutation } from '../shared/state/mutation';
import { toRfidReader } from './rfid-reader.mapper';
import { DeleteRfidReaderCommand } from './rfid-reader.model';

export const RfidReadersStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const rfidReadersApi = inject(RfidReadersApiService);
    return {
      _rfidReadersApi: rfidReadersApi,
      // loads on first use of the store
      _rfidReadersResource: rfidReadersApi.rfidReadersResource(),
    };
  }),
  withComputed(({ _rfidReadersResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_rfidReadersResource.hasValue() ? _rfidReadersResource.value().map(toRfidReader) : [])),
    isLoading: computed(() => _rfidReadersResource.isLoading()),
    hasLoadError: computed(() => !!_rfidReadersResource.error()),
  })),
  withMethods((store) => ({
    reloadRfidReaders(): void {
      store._rfidReadersResource.reload();
    },
    deleteRfidReader: mutation<DeleteRfidReaderCommand>({
      request: (command) => store._rfidReadersApi.deleteRfidReader(command.id),
      onSuccess: () => store._rfidReadersResource.reload(),
    }),
  })),
);
