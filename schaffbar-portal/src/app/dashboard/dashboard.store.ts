import { computed, inject } from '@angular/core';

import { signalStore, withComputed, withMethods, withProps } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { interval, pipe, switchMap, tap } from 'rxjs';

import { CustomersApiService } from '../shared/api/customers/customers.api-service';
import { RfidReadersApiService } from '../shared/api/rfid-readers/rfid-readers.api-service';
import { ToolDashboardApiService } from '../shared/api/tool-dashboard/tool-dashboard.api-service';
import { ToolsApiService } from '../shared/api/tools/tools.api-service';
import { WorkshopDashboardApiService } from '../shared/api/workshop-dashboard/workshop-dashboard.api-service';
import { toActiveTool, toActiveUser, toRfidReader, toTool, toUser } from './dashboard.mapper';
import { isHardwareSetUp, isMinor, needsRfidReaderType } from './dashboard.model';

export const DashboardStore = signalStore(
  withProps(() => ({
    // all load when the overview opens
    _activeUsersResource: inject(WorkshopDashboardApiService).activeUsersResource(),
    _activeToolsResource: inject(ToolDashboardApiService).activeToolsResource(),
    _usersResource: inject(CustomersApiService).customersResource(),
    _toolsResource: inject(ToolsApiService).toolsResource(),
    _rfidReadersResource: inject(RfidReadersApiService).rfidReadersResource(),
  })),
  withProps((store) => ({
    _resources: [
      store._activeUsersResource,
      store._activeToolsResource,
      store._toolsResource,
      store._usersResource,
      store._rfidReadersResource,
    ],
  })),
  withComputed(
    ({
      _activeUsersResource,
      _activeToolsResource,
      _toolsResource,
      _usersResource,
      _rfidReadersResource,
      _resources,
    }) => ({
      activeUsers: computed(() =>
        _activeUsersResource.hasValue() ? _activeUsersResource.value().map(toActiveUser) : [],
      ),
      activeTools: computed(() =>
        _activeToolsResource.hasValue() ? _activeToolsResource.value().map(toActiveTool) : [],
      ),
      tools: computed(() => (_toolsResource.hasValue() ? _toolsResource.value().map(toTool) : [])),
      users: computed(() => (_usersResource.hasValue() ? _usersResource.value().map(toUser) : [])),
      rfidReaders: computed(() =>
        _rfidReadersResource.hasValue() ? _rfidReadersResource.value().map(toRfidReader) : [],
      ),
      // only the first load – the overview reloads itself every minute, the progress bar would flicker
      isLoading: computed(() => _resources.some((resource) => resource.status() === 'loading')),
      hasLoadError: computed(() => _resources.some((resource) => !!resource.error())),
      // when the people in the workshop were loaded last – read again with every new answer
      updatedAt: computed(() => (_activeUsersResource.hasValue() && _activeUsersResource.value() ? new Date() : null)),
    }),
  ),
  withComputed(({ activeUsers, users, tools, rfidReaders }) => ({
    minorsInWorkshopCount: computed(() => {
      const minorIds = new Set(
        users()
          .filter(isMinor)
          .map((user) => user.id),
      );
      return activeUsers().filter((activeUser) => minorIds.has(activeUser.customerId)).length;
    }),
    toolsWithoutHardware: computed(() => tools().filter((tool) => !isHardwareSetUp(tool))),
    rfidReadersWithoutType: computed(() => rfidReaders().filter(needsRfidReaderType)),
  })),
  withMethods((store) => ({
    reloadDashboard(): void {
      store._resources.forEach((resource) => resource.reload());
    },
  })),
  withMethods((store) => ({
    // reloads everything every `intervalMs` while the overview is open – it ends with the page
    pollDashboard: rxMethod<number>(
      pipe(switchMap((intervalMs) => interval(intervalMs).pipe(tap(() => store.reloadDashboard())))),
    ),
  })),
);
