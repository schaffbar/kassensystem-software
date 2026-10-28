import { HttpResourceRef, httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import { environment } from '../../environments/environment';
import { ActiveTool, ActiveUser } from './dashboard.model';

const DASHBOARD_API_URL = `${environment.apiBaseUrl}/api/v1/workshop-dashboard`;
const DASHBOARD_TOOL_API_URL = `${environment.apiBaseUrl}/api/v1/tool-dashboard`;

@Service()
export class DashboardService {
  // --------------------------------------------------------------------------
  // queries

  /**
   * Loads the users currently in the workshop; reload with `reload()`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  activeUsersResource(): HttpResourceRef<ActiveUser[] | undefined> {
    return httpResource<ActiveUser[]>(() => `${DASHBOARD_API_URL}/active-users`);
  }

  /**
   * Loads the tools currently in use; reload with `reload()`.
   * Must be called in an injection context (e.g. a field initializer or `withProps` of a signal store).
   */
  activeToolsResource(): HttpResourceRef<ActiveTool[] | undefined> {
    return httpResource<ActiveTool[]>(() => `${DASHBOARD_TOOL_API_URL}/active-tools`);
  }
}
