import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { RfidReadersApiService } from '../../../shared/api/rfid-readers/rfid-readers.api-service';
import { mutation } from '../../../shared/state/mutation';
import { toChangeRfidReaderTypeRequest, toRfidReader, toUpdateRfidReaderRequest } from '../../rfid-reader.mapper';
import { ChangeRfidReaderTypeCommand, UpdateRfidReaderCommand } from '../../rfid-reader.model';

interface RfidReaderDetailState {
  rfidReaderId: string | null;
}

export const RfidReaderDetailStore = signalStore(
  withState<RfidReaderDetailState>({ rfidReaderId: null }),
  withProps((store) => {
    const rfidReadersApi = inject(RfidReadersApiService);
    return {
      _rfidReadersApi: rfidReadersApi,
      // (re)loads whenever the id changes – replaces the dirty flag + effect + rxMethod
      _rfidReaderResource: rfidReadersApi.rfidReaderResource(store.rfidReaderId),
    };
  }),
  withComputed(({ _rfidReaderResource }) => ({
    // hasValue() stays true while reloading, so the page keeps showing the reader instead of flickering
    rfidReader: computed(() => (_rfidReaderResource.hasValue() ? toRfidReader(_rfidReaderResource.value()) : null)),
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
      request: (command) => store._rfidReadersApi.updateRfidReader(command.id, toUpdateRfidReaderRequest(command)),
      onSuccess: () => store._rfidReaderResource.reload(),
    }),
    changeRfidReaderType: mutation<ChangeRfidReaderTypeCommand>({
      request: (command) =>
        store._rfidReadersApi.changeRfidReaderType(command.id, toChangeRfidReaderTypeRequest(command)),
      onSuccess: () => store._rfidReaderResource.reload(),
    }),
  })),
);
