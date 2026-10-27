import { computed, inject } from '@angular/core';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import {
  BatchCreateToolCertificationCommand,
  DeleteToolCertificationCommand,
} from '../../../users/shared/models/tool-certification.model';
import { mutation } from '../../../shared/state/mutation';
import { ToolCertificationService } from '../../../users/shared/services/tool-certification.service';
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
    const toolCertificationService = inject(ToolCertificationService);
    return {
      _toolsService: toolsService,
      _toolCertificationService: toolCertificationService,
      // (re)load whenever the id changes – replaces the dirty flag + effect + rxMethods
      _toolResource: toolsService.toolResource(store.toolId),
      _certificationsResource: toolCertificationService.certificationsByToolResource(store.toolId),
    };
  }),
  withComputed(({ _toolResource, _certificationsResource }) => ({
    // hasValue() stays true while reloading, so the page keeps showing the data instead of flickering
    tool: computed(() => (_toolResource.hasValue() ? _toolResource.value() : null)),
    toolCertifications: computed(() => (_certificationsResource.hasValue() ? _certificationsResource.value() : [])),
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
      request: (command) => store._toolCertificationService.batchCreate(command),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    deleteCertification: mutation<DeleteToolCertificationCommand>({
      request: (command) => store._toolCertificationService.delete(command),
      onSuccess: () => store._certificationsResource.reload(),
    }),
  })),
);
