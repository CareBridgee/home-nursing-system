import { Routes } from '@angular/router';
import { AdminShellComponent } from './pages/admin-shell/admin-shell.component';

/**
 * Drop this into your top-level app.routes.ts, e.g.:
 *
 *   { path: 'admin', children: ADMIN_ROUTES }
 *
 * or lazy-load the whole thing:
 *
 *   { path: 'admin', loadChildren: () => import('./admin/admin.routes').then(m => m.ADMIN_ROUTES) }
 *
 * Add an auth guard on the shell route once your admin auth is wired up, e.g.:
 *   { path: '', component: AdminShellComponent, canActivate: [adminGuard], children: [...] }
 */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      { path: '', redirectTo: 'nurses', pathMatch: 'full' },
      {
        path: 'nurses',
        loadComponent: () =>
          import('./pages/nurse-approvals-page/nurse-approvals-page.component').then(
            (m) => m.NurseApprovalsPageComponent,
          ),
      },
      {
        path: 'medications',
        loadComponent: () =>
          import('./pages/medications-page/medications-page.component').then(
            (m) => m.MedicationsPageComponent,
          ),
      },
      {
        path: 'service-types',
        loadComponent: () =>
          import('./pages/service-types-page/service-types-page.component').then(
            (m) => m.ServiceTypesPageComponent,
          ),
      },
      {
        path: 'medical-conditions',
        loadComponent: () =>
          import('./pages/medical-conditions-page/medical-conditions-page.component').then(
            (m) => m.MedicalConditionsPageComponent,
          ),
      },
      {
        path: 'allergies',
        loadComponent: () =>
          import('./pages/allergies-page/allergies-page.component').then(
            (m) => m.AllergiesPageComponent,
          ),
      },
    ],
  },
];
