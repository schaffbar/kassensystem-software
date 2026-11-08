import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';

import { CustomersApiService } from '../shared/api/customers/customers.api-service';
import { RfidTagAssignmentsApiService } from '../shared/api/rfid-tag-assignments/rfid-tag-assignments.api-service';
import { RfidTagsApiService } from '../shared/api/rfid-tags/rfid-tags.api-service';
import { mutation } from '../shared/state/mutation';
import { toRfidTags, toUnassignRfidTagRequest, toWaitingUser } from './rfid-tag.mapper';
import { DeleteRfidTagCommand, UnassignRfidTagCommand } from './rfid-tag.model';

export const RfidTagsStore = signalStore(
  { providedIn: 'root' },
  withProps(() => {
    const rfidTagsApi = inject(RfidTagsApiService);
    const rfidTagAssignmentsApi = inject(RfidTagAssignmentsApiService);
    return {
      _rfidTagsApi: rfidTagsApi,
      _rfidTagAssignmentsApi: rfidTagAssignmentsApi,
      // all load on first use of the store; assignments and users tell who has which tag
      _rfidTagsResource: rfidTagsApi.rfidTagsResource(),
      _assignmentsResource: rfidTagAssignmentsApi.rfidTagAssignmentsResource(),
      _usersResource: inject(CustomersApiService).customersResource(),
    };
  }),
  withComputed(({ _rfidTagsResource, _assignmentsResource, _usersResource }) => {
    const assignments = computed(() => (_assignmentsResource.hasValue() ? _assignmentsResource.value() : []));
    const users = computed(() => (_usersResource.hasValue() ? _usersResource.value() : []));
    return {
      // hasValue() stays true while reloading, so the list keeps showing the entries instead of flickering
      rfidTags: computed(() =>
        _rfidTagsResource.hasValue() ? toRfidTags(_rfidTagsResource.value(), assignments(), users()) : [],
      ),
      waitingUser: computed(() => toWaitingUser(assignments(), users())),
      isLoading: computed(
        () => _rfidTagsResource.isLoading() || _assignmentsResource.isLoading() || _usersResource.isLoading(),
      ),
      hasLoadError: computed(
        () => !!_rfidTagsResource.error() || !!_assignmentsResource.error() || !!_usersResource.error(),
      ),
    };
  }),
  withMethods((store) => ({
    reloadRfidTags(): void {
      store._rfidTagsResource.reload();
      store._assignmentsResource.reload();
      store._usersResource.reload();
    },
    unassignRfidTag: mutation<UnassignRfidTagCommand>({
      request: (command) => store._rfidTagAssignmentsApi.unassignRfidTag(toUnassignRfidTagRequest(command)),
      onSuccess: () => store._assignmentsResource.reload(),
    }),
    deleteRfidTag: mutation<DeleteRfidTagCommand>({
      request: (command) => store._rfidTagsApi.deleteRfidTag(command.rfidTagId),
      onSuccess: () => store._rfidTagsResource.reload(),
    }),
  })),
);
