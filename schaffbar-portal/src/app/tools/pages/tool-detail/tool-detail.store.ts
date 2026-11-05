import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { ToolCertificationsApiService } from '../../../shared/api/tool-certifications/tool-certifications.api-service';
import { CustomersApiService } from '../../../shared/api/customers/customers.api-service';
import { RfidReadersApiService } from '../../../shared/api/rfid-readers/rfid-readers.api-service';
import { ToolsApiService } from '../../../shared/api/tools/tools.api-service';
import { mutation } from '../../../shared/state/mutation';
import { toBatchCertifyRequest, toToolCertification } from '../../tool-certification.mapper';
import { toRfidReader } from '../../rfid-reader.mapper';
import { toUser } from '../../user.mapper';
import { toInstructorsRequest, toSetWlanRelaisRequest, toTool, toUpdateToolRequest } from '../../tool.mapper';
import { BatchCreateToolCertificationCommand, DeleteToolCertificationCommand } from '../../tool-certification.model';
import {
  AddToolInstructorsCommand,
  ChangeRfidReaderCommand,
  ClearRfidReaderCommand,
  ClearWlanRelaisCommand,
  RemoveToolInstructorsCommand,
  SetWlanRelaisCommand,
  UpdateToolCommand,
} from '../../tool.model';

interface ToolDetailState {
  toolId: string | null;
}

export const ToolDetailStore = signalStore(
  withState<ToolDetailState>({ toolId: null }),
  withProps((store) => {
    const toolsApi = inject(ToolsApiService);
    const toolCertificationsApi = inject(ToolCertificationsApiService);
    return {
      _toolsApi: toolsApi,
      _toolCertificationsApi: toolCertificationsApi,
      // (re)load whenever the id changes
      _toolResource: toolsApi.toolResource(store.toolId),
      _certificationsResource: toolCertificationsApi.toolCertificationsByToolResource(store.toolId),
      _rfidReadersResource: inject(RfidReadersApiService).rfidReadersResource(),
      _usersResource: inject(CustomersApiService).customersResource(),
    };
  }),
  withComputed(({ _toolResource, _certificationsResource, _rfidReadersResource, _usersResource }) => {
    const resources = [_toolResource, _certificationsResource, _rfidReadersResource, _usersResource];
    return {
      // hasValue() stays true while reloading, so the page keeps showing the data instead of flickering
      tool: computed(() => (_toolResource.hasValue() ? toTool(_toolResource.value()) : null)),
      toolCertifications: computed(() =>
        _certificationsResource.hasValue() ? _certificationsResource.value().map(toToolCertification) : [],
      ),
      rfidReaders: computed(() =>
        _rfidReadersResource.hasValue() ? _rfidReadersResource.value().map(toRfidReader) : [],
      ),
      users: computed(() => (_usersResource.hasValue() ? _usersResource.value().map(toUser) : [])),
      isLoading: computed(() => resources.some((resource) => resource.isLoading())),
      hasLoadError: computed(() => resources.some((resource) => !!resource.error())),
    };
  }),
  withMethods((store) => ({
    setToolId: signalMethod<string>((toolId) => {
      patchState(store, { toolId });
    }),
    reloadTool(): void {
      store._toolResource.reload();
      store._certificationsResource.reload();
      store._rfidReadersResource.reload();
      store._usersResource.reload();
    },
  })),
  // mutations of the tool – reload the tool on success
  withMethods((store) => ({
    updateTool: mutation<UpdateToolCommand>({
      request: (command) => store._toolsApi.updateTool(command.id, toUpdateToolRequest(command)),
      onSuccess: () => store._toolResource.reload(),
    }),
    changeRfidReader: mutation<ChangeRfidReaderCommand>({
      request: (command) => store._toolsApi.assignRfidReader(command.toolId, command.rfidReaderId),
      onSuccess: () => store._toolResource.reload(),
    }),
    clearRfidReader: mutation<ClearRfidReaderCommand>({
      request: (command) => store._toolsApi.clearRfidReader(command.toolId),
      onSuccess: () => store._toolResource.reload(),
    }),
    setWlanRelais: mutation<SetWlanRelaisCommand>({
      request: (command) => store._toolsApi.setWlanRelais(command.toolId, toSetWlanRelaisRequest(command)),
      onSuccess: () => store._toolResource.reload(),
    }),
    clearWlanRelais: mutation<ClearWlanRelaisCommand>({
      request: (command) => store._toolsApi.clearWlanRelais(command.toolId),
      onSuccess: () => store._toolResource.reload(),
    }),
    addInstructors: mutation<AddToolInstructorsCommand>({
      request: (command) => store._toolsApi.addInstructors(command.toolId, toInstructorsRequest(command)),
      onSuccess: () => store._toolResource.reload(),
    }),
    removeInstructors: mutation<RemoveToolInstructorsCommand>({
      request: (command) => store._toolsApi.removeInstructors(command.toolId, toInstructorsRequest(command)),
      onSuccess: () => store._toolResource.reload(),
    }),
  })),
  // mutations of the certifications – reload the certifications on success
  withMethods((store) => ({
    batchCreateCertifications: mutation<BatchCreateToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.batchCertify(toBatchCertifyRequest(command)),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    deleteCertification: mutation<DeleteToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.deleteToolCertification(command.customerId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
  })),
);
