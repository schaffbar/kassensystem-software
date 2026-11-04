import { HttpResourceRef, httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

import * as z from 'zod';

import { API_URL } from '../api-url';
import { parseWith } from '../parse';
import { ToolDashboardEntryApiModel, toolDashboardEntrySchema } from './tool-dashboard.api-model';

const TOOL_DASHBOARD_API_URL = `${API_URL}/tool-dashboard`;

@Service()
export class ToolDashboardApiService {
  // --------------------------------------------------------------------------
  // queries

  activeToolsResource(): HttpResourceRef<ToolDashboardEntryApiModel[] | undefined> {
    return httpResource(() => `${TOOL_DASHBOARD_API_URL}/active-tools`, {
      parse: parseWith(z.array(toolDashboardEntrySchema), 'GET /tool-dashboard/active-tools'),
    });
  }
}
