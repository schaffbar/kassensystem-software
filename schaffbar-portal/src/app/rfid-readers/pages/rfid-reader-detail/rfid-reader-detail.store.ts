import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { mutation } from '../../../shared/state/mutation';
import { ChangeRfidReaderTypeCommand, UpdateRfidReaderCommand } from '../../rfid-reader.model';
import { RfidReaderService } from '../../rfid-readers.service';

interface RfidReaderDetailState {
  rfidReaderId: string | null;
}

export const RfidReaderDetailStore = signalStore(
  withState<RfidReaderDetailState>({ rfidReaderId: null }),
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
    updateRfidReader: mutation<UpdateRfidReaderCommand>({
      request: (command) => store._rfidReaderService.updateRfidReader(command),
      onSuccess: () => store._rfidReaderResource.reload(),
    }),
    changeRfidReaderType: mutation<ChangeRfidReaderTypeCommand>({
      request: (command) => store._rfidReaderService.changeRfidReaderType(command),
      onSuccess: () => store._rfidReaderResource.reload(),
    }),
  })),
);
