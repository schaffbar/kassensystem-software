import { HttpResourceRef, httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import { WorkshopDashboardEntryApiModel, workshopDashboardEntrySchema } from './workshop-dashboard.api-model';

const WORKSHOP_DASHBOARD_API_URL = `${API_URL}/workshop-dashboard`;

@Service()
export class WorkshopDashboardApiService {
  // --------------------------------------------------------------------------
  // queries

  activeUsersResource(): HttpResourceRef<WorkshopDashboardEntryApiModel[] | undefined> {
    return httpResource(() => `${WORKSHOP_DASHBOARD_API_URL}/active-users`, {
      parse: parseWith(z.array(workshopDashboardEntrySchema), 'GET /workshop-dashboard/active-users'),
    });
  }
}
