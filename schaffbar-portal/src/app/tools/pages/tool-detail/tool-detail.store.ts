import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { ToolCertificationsApiService } from '../../../shared/api/tool-certifications/tool-certifications.api-service';
import { mutation } from '../../../shared/state/mutation';
import { toBatchCertifyRequest, toToolCertification } from '../../tool-certification.mapper';
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
import { ToolsService } from '../../tools.service';

interface ToolDetailState {
  toolId: string | null;
}

export const ToolDetailStore = signalStore(
  withState<ToolDetailState>({ toolId: null }),
  withProps((store) => {
    const toolsService = inject(ToolsService);
    const toolCertificationsApi = inject(ToolCertificationsApiService);
    return {
      _toolsService: toolsService,
      _toolCertificationsApi: toolCertificationsApi,
      // (re)load whenever the id changes – replaces the dirty flag + effect + rxMethods
      _toolResource: toolsService.toolResource(store.toolId),
      _certificationsResource: toolCertificationsApi.certificationsByToolResource(store.toolId),
    };
  }),
  withComputed(({ _toolResource, _certificationsResource }) => ({
    // hasValue() stays true while reloading, so the page keeps showing the data instead of flickering
    tool: computed(() => (_toolResource.hasValue() ? _toolResource.value() : null)),
    toolCertifications: computed(() =>
      _certificationsResource.hasValue() ? _certificationsResource.value().map(toToolCertification) : [],
    ),
    isLoading: computed(() => _toolResource.isLoading() || _certificationsResource.isLoading()),
    hasLoadError: computed(() => !!_toolResource.error() || !!_certificationsResource.error()),
  })),
  withMethods((store) => ({
    setToolId: signalMethod<string>((toolId) => {
      patchState(store, { toolId });
    }),
    reloadTool(): void {
      store._toolResource.reload();
      store._certificationsResource.reload();
    },
  })),
  // mutations of the tool – reload the tool on success
  withMethods((store) => ({
    updateTool: mutation<UpdateToolCommand>({
      request: (command) => store._toolsService.updateTool(command),
      onSuccess: () => store._toolResource.reload(),
    }),
    changeRfidReader: mutation<ChangeRfidReaderCommand>({
      request: (command) => store._toolsService.changeRfidReader(command),
      onSuccess: () => store._toolResource.reload(),
    }),
    clearRfidReader: mutation<ClearRfidReaderCommand>({
      request: (command) => store._toolsService.clearRfidReader(command),
      onSuccess: () => store._toolResource.reload(),
    }),
    setWlanRelais: mutation<SetWlanRelaisCommand>({
      request: (command) => store._toolsService.setWlanRelais(command),
      onSuccess: () => store._toolResource.reload(),
    }),
    clearWlanRelais: mutation<ClearWlanRelaisCommand>({
      request: (command) => store._toolsService.clearWlanRelais(command),
      onSuccess: () => store._toolResource.reload(),
    }),
    addInstructors: mutation<AddToolInstructorsCommand>({
      request: (command) => store._toolsService.addInstructors(command),
      onSuccess: () => store._toolResource.reload(),
    }),
    removeInstructors: mutation<RemoveToolInstructorsCommand>({
      request: (command) => store._toolsService.removeInstructors(command),
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
      request: (command) => store._toolCertificationsApi.delete(command.customerId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
  })),
);
