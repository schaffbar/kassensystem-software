import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { mutation } from '../shared/state/mutation';
import { DeleteRfidReaderCommand } from './rfid-reader.model';
import { RfidReaderService } from './rfid-readers.service';

export const RfidReadersStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const rfidReaderService = inject(RfidReaderService);
    return {
      _rfidReaderService: rfidReaderService,
      // loads on first use of the store – replaces withEntities + rxMethod + onInit
      _rfidReadersResource: rfidReaderService.rfidReadersResource(),
    };
  }),
  withComputed(({ _rfidReadersResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_rfidReadersResource.hasValue() ? _rfidReadersResource.value() : [])),
    isLoading: computed(() => _rfidReadersResource.isLoading()),
    hasLoadError: computed(() => !!_rfidReadersResource.error()),
  })),
  withMethods((store) => ({
    reloadRfidReaders(): void {
      store._rfidReadersResource.reload();
    },
    deleteRfidReader: mutation<DeleteRfidReaderCommand>({
      request: (command) => store._rfidReaderService.deleteRfidReader(command),
      onSuccess: () => store._rfidReadersResource.reload(),
    }),
  })),
);
