import { Routes } from '@angular/router';

export const ROUTE = {
  DASHBOARD: 'dashboard',
  USERS: 'users',
  TOOLS: 'tools',
  RFID_READERS: 'rfid-readers',
  RFID_TAGS: 'rfid-tags',
} as const;

// route titles are translation keys, translated by TranslatedTitleStrategy

export const routes: Routes = [
  {
    path: ROUTE.DASHBOARD,
    title: 'sidebar.dashboard',
    loadComponent: () => import('./dashboard/pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: ROUTE.USERS,
    title: 'sidebar.users',
    loadComponent: () => import('./users/pages/user-list/user-list.component').then((m) => m.UserListComponent),
  },
  {
    path: `${ROUTE.USERS}/:id`,
    title: 'sidebar.users',
    loadComponent: () => import('./users/pages/user-detail/user-detail.component').then((m) => m.UserDetailComponent),
  },
  {
    path: ROUTE.TOOLS,
    title: 'sidebar.tools',
    loadComponent: () => import('./tools/pages/tool-list/tool-list.component').then((m) => m.ToolListComponent),
  },
  {
    path: `${ROUTE.TOOLS}/:id`,
    title: 'sidebar.tools',
    loadComponent: () => import('./tools/pages/tool-detail/tool-detail.component').then((m) => m.ToolDetailComponent),
  },
  {
    path: ROUTE.RFID_READERS,
    title: 'sidebar.rfidReaders',
    loadComponent: () =>
      import('./rfid-readers/pages/rfid-reader-list/rfid-reader-list.component').then((m) => m.RfidReaderListComponent),
  },
  {
    path: `${ROUTE.RFID_READERS}/:id`,
    title: 'sidebar.rfidReaders',
    loadComponent: () =>
      import('./rfid-readers/pages/rfid-reader-detail/rfid-reader-detail.component').then(
        (m) => m.RfidReaderDetailComponent,
      ),
  },
  {
    path: ROUTE.RFID_TAGS,
    title: 'sidebar.rfidTags',
    loadComponent: () =>
      import('./rfid-tags/pages/rfid-tag-list/rfid-tag-list.component').then((m) => m.RfidTagListComponent),
  },
  {
    path: '',
    redirectTo: ROUTE.DASHBOARD,
    pathMatch: 'full',
  },
  {
    path: '**',
    title: 'notFound.title',
    loadComponent: () => import('./core/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
];
