import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { RfidTagsApiService } from '../shared/api/rfid-tags/rfid-tags.api-service';
import { mutation } from '../shared/state/mutation';
import { toRfidTag } from './rfid-tag.mapper';
import { DeleteRfidTagCommand } from './rfid-tag.model';

export const RfidTagsStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const rfidTagsApi = inject(RfidTagsApiService);
    return {
      _rfidTagsApi: rfidTagsApi,
      // loads on first use of the store
      _rfidTagsResource: rfidTagsApi.rfidTagsResource(),
    };
  }),
  withComputed(({ _rfidTagsResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_rfidTagsResource.hasValue() ? _rfidTagsResource.value().map(toRfidTag) : [])),
    isLoading: computed(() => _rfidTagsResource.isLoading()),
    hasLoadError: computed(() => !!_rfidTagsResource.error()),
  })),
  withMethods((store) => ({
    reloadRfidTags(): void {
      store._rfidTagsResource.reload();
    },
    deleteRfidTag: mutation<DeleteRfidTagCommand>({
      request: (command) => store._rfidTagsApi.deleteRfidTag(command.rfidTagId),
      onSuccess: () => store._rfidTagsResource.reload(),
    }),
  })),
);
