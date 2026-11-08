import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { RfidReadersApiService } from '../shared/api/rfid-readers/rfid-readers.api-service';
import { ToolsApiService } from '../shared/api/tools/tools.api-service';
import { mutation } from '../shared/state/mutation';
import { toRfidReaderListItems } from './rfid-reader.mapper';
import { DeleteRfidReaderCommand } from './rfid-reader.model';

export const RfidReadersStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const rfidReadersApi = inject(RfidReadersApiService);
    return {
      _rfidReadersApi: rfidReadersApi,
      // both load on first use of the store; the tools tell which switch box belongs to which tool
      _rfidReadersResource: rfidReadersApi.rfidReadersResource(),
      _toolsResource: inject(ToolsApiService).toolsResource(),
    };
  }),
  withComputed(({ _rfidReadersResource, _toolsResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    rfidReaders: computed(() =>
      _rfidReadersResource.hasValue()
        ? toRfidReaderListItems(_rfidReadersResource.value(), _toolsResource.hasValue() ? _toolsResource.value() : [])
        : [],
    ),
    isLoading: computed(() => _rfidReadersResource.isLoading() || _toolsResource.isLoading()),
    hasLoadError: computed(() => !!_rfidReadersResource.error() || !!_toolsResource.error()),
  })),
  withMethods((store) => ({
    reloadRfidReaders(): void {
      store._rfidReadersResource.reload();
      store._toolsResource.reload();
    },
    deleteRfidReader: mutation<DeleteRfidReaderCommand>({
      request: (command) => store._rfidReadersApi.deleteRfidReader(command.rfidReaderId),
      onSuccess: () => store._rfidReadersResource.reload(),
    }),
  })),
);
