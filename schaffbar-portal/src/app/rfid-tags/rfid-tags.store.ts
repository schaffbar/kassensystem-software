import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { mutation } from '../shared/state/mutation';
import { DeleteRfidTagCommand } from './rfid-tag.model';
import { RfidTagService } from './rfid-tags.service';

export const RfidTagsStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const rfidTagService = inject(RfidTagService);
    return {
      _rfidTagService: rfidTagService,
      // loads on first use of the store – replaces withEntities + rxMethod + onInit
      _rfidTagsResource: rfidTagService.rfidTagsResource(),
    };
  }),
  withComputed(({ _rfidTagsResource }) => ({
    // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
    entities: computed(() => (_rfidTagsResource.hasValue() ? _rfidTagsResource.value() : [])),
    isLoading: computed(() => _rfidTagsResource.isLoading()),
    hasLoadError: computed(() => !!_rfidTagsResource.error()),
  })),
  withMethods((store) => ({
    reloadRfidTags(): void {
      store._rfidTagsResource.reload();
    },
    deleteRfidTag: mutation<DeleteRfidTagCommand>({
      request: (command) => store._rfidTagService.deleteRfidTag(command),
      onSuccess: () => store._rfidTagsResource.reload(),
    }),
  })),
);
