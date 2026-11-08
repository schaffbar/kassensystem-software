import { computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { patchState, signalMethod, signalStore, withComputed, withMethods, withProps, withState } from '@ngrx/signals';

import { ROUTE } from '../../../app.routes';
import { ToolCertificationsApiService } from '../../../shared/api/tool-certifications/tool-certifications.api-service';
import { CustomersApiService } from '../../../shared/api/customers/customers.api-service';
import { RfidReadersApiService } from '../../../shared/api/rfid-readers/rfid-readers.api-service';
import { ToolsApiService } from '../../../shared/api/tools/tools.api-service';
import { RfidReaderType } from '../../../shared/models/rfid-reader-type';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { mutation } from '../../../shared/state/mutation';
import { toBatchCertifyRequest, toToolCertification } from '../../tool-certification.mapper';
import { toRfidReader } from '../../rfid-reader.mapper';
import { toUser } from '../../user.mapper';
import { toInstructorsRequest, toSetWlanRelaisRequest, toTool, toUpdateToolRequest } from '../../tool.mapper';
import { BatchCreateToolCertificationsCommand, DeleteToolCertificationCommand } from '../../tool-certification.model';
import { InstructorCandidate, ToolInstructor } from '../../user.model';
import {
  AddToolInstructorsCommand,
  ChangeRfidReaderCommand,
  ClearRfidReaderCommand,
  ClearWlanRelaisCommand,
  DeleteToolCommand,
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
      _router: inject(Router),
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
      _rfidReaders: computed(() =>
        _rfidReadersResource.hasValue() ? _rfidReadersResource.value().map(toRfidReader) : [],
      ),
      users: computed(() => (_usersResource.hasValue() ? _usersResource.value().map(toUser) : [])),
      isLoading: computed(() => resources.some((resource) => resource.isLoading())),
      hasLoadError: computed(() => resources.some((resource) => !!resource.error())),
    };
  }),
  withComputed(({ tool, users, toolCertifications, _rfidReaders }) => {
    const instructorIds = computed(() => new Set(tool()?.instructors ?? []));
    const instructors = computed<ToolInstructor[]>(() =>
      users()
        .filter((user) => instructorIds().has(user.id))
        .map((user) => ({
          ...user,
          certificationsGiven: toolCertifications().filter((certification) => certification.certifiedBy === user.id)
            .length,
        })),
    );
    return {
      // only a switch box can switch a tool on and off
      assignableRfidReaders: computed(() =>
        _rfidReaders().filter((reader) => reader.type === RfidReaderType.SwitchBox),
      ),
      assignedRfidReader: computed(() => _rfidReaders().find((reader) => reader.id === tool()?.rfidReaderId) ?? null),
      activeCertificationsCount: computed(
        () =>
          toolCertifications().filter((certification) => certification.status === ToolCertificationStatus.Active)
            .length,
      ),
      instructors,
      instructorCandidates: computed<InstructorCandidate[]>(() => {
        const certifications = new Map(
          toolCertifications().map((certification) => [certification.customerId, certification]),
        );
        return users()
          .filter((user) => !instructorIds().has(user.id))
          .map((user) => {
            const certification = certifications.get(user.id);
            return {
              ...user,
              certification: certification
                ? { status: certification.status, certifiedAt: certification.certifiedAt }
                : null,
            };
          });
      }),
      // only the instructors of the tool certify
      certifiers: instructors,
      // a user is certified once per tool; instructors need no certification
      certifiableUsers: computed(() => {
        const certifiedIds = new Set(toolCertifications().map((certification) => certification.customerId));
        return users().filter((user) => !instructorIds().has(user.id) && !certifiedIds.has(user.id));
      }),
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
      request: (command) => store._toolsApi.updateTool(command.toolId, toUpdateToolRequest(command)),
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
    addToolInstructors: mutation<AddToolInstructorsCommand>({
      request: (command) => store._toolsApi.addInstructors(command.toolId, toInstructorsRequest(command)),
      onSuccess: () => store._toolResource.reload(),
    }),
    removeToolInstructors: mutation<RemoveToolInstructorsCommand>({
      request: (command) => store._toolsApi.removeInstructors(command.toolId, toInstructorsRequest(command)),
      onSuccess: () => store._toolResource.reload(),
    }),
    // the page has nothing left to show, so it goes back to the list
    deleteTool: mutation<DeleteToolCommand>({
      request: (command) => store._toolsApi.deleteTool(command.toolId),
      onSuccess: () => void store._router.navigate([ROUTE.TOOLS]),
    }),
  })),
  // mutations of the certifications – reload the certifications on success
  withMethods((store) => ({
    batchCreateToolCertifications: mutation<BatchCreateToolCertificationsCommand>({
      request: (command) => store._toolCertificationsApi.certifyCustomers(toBatchCertifyRequest(command)),
      onSuccess: () => store._certificationsResource.reload(),
    }),
    deleteToolCertification: mutation<DeleteToolCertificationCommand>({
      request: (command) => store._toolCertificationsApi.deleteToolCertification(command.userId, command.toolId),
      onSuccess: () => store._certificationsResource.reload(),
    }),
  })),
);
