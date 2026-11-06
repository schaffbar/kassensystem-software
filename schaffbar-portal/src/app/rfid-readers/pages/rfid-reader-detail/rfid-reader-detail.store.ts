import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';
import { map, Observable, of } from 'rxjs';

import { RfidReadersApiService } from '../../../shared/api/rfid-readers/rfid-readers.api-service';
import { ToolsApiService } from '../../../shared/api/tools/tools.api-service';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
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
      _toolsApi: inject(ToolsApiService),
      // (re)loads whenever the id changes
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
    // a switch box keeps its type while a tool is assigned to it – the tool would lose its reader
    canChangeRfidReaderType(): Observable<boolean> {
      const rfidReader = store.rfidReader();
      if (!rfidReader) return of(false);
      if (rfidReader.type !== RfidReaderType.SwitchBox) return of(true);
      return store._toolsApi.getToolsByRfidReader(rfidReader.id).pipe(map((tools) => tools.length === 0));
    },
    updateRfidReader: mutation<UpdateRfidReaderCommand>({
      request: (command) =>
        store._rfidReadersApi.updateRfidReader(command.rfidReaderId, toUpdateRfidReaderRequest(command)),
      onSuccess: () => store._rfidReaderResource.reload(),
    }),
    changeRfidReaderType: mutation<ChangeRfidReaderTypeCommand>({
      request: (command) =>
        store._rfidReadersApi.changeRfidReaderType(command.rfidReaderId, toChangeRfidReaderTypeRequest(command)),
      onSuccess: () => store._rfidReaderResource.reload(),
    }),
  })),
);
