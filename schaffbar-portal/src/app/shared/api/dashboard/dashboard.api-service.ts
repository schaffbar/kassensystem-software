import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Service } from '@angular/core';

import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import {
  ActiveToolUsageApiModel,
  activeToolUsageSchema,
  ActiveWorkshopUserApiModel,
  activeWorkshopUserSchema,
} from './dashboard.api-model';

const DASHBOARD_API_URL = `${API_URL}/dashboard`;

@Service()
export class DashboardApiService {
  // --------------------------------------------------------------------------
  // queries

  activeUsersResource(): HttpResourceRef<ActiveWorkshopUserApiModel[] | undefined> {
    return httpResource(() => `${DASHBOARD_API_URL}/active-users`, {
      parse: parseWith(z.array(activeWorkshopUserSchema), 'GET /dashboard/active-users'),
    });
  }

  activeToolsResource(): HttpResourceRef<ActiveToolUsageApiModel[] | undefined> {
    return httpResource(() => `${DASHBOARD_API_URL}/active-tools`, {
      parse: parseWith(z.array(activeToolUsageSchema), 'GET /dashboard/active-tools'),
    });
  }
}
