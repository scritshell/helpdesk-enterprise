import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/auth/auth.guards';
import { AppShellComponent } from './core/layout/app-shell.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LoginComponent } from './features/auth/login.component';
import { TicketListComponent } from './features/tickets/ticket-list.component';
import { TicketCreateComponent } from './features/tickets/ticket-create.component';
import { TicketDetailComponent } from './features/tickets/ticket-detail.component';
import { TechnicianQueueComponent } from './features/tickets/technician-queue.component';
import { AdminUsersComponent } from './features/admin/admin-users.component';
import { AdminCategoriesComponent } from './features/admin/admin-categories.component';
import { AdminStatisticsComponent } from './features/admin/admin-statistics.component';

export const appRoutes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'app', component: AppShellComponent, canActivate: [authGuard], children: [
    { path: 'dashboard', component: DashboardComponent },
    { path: 'tickets', component: TicketListComponent },
    { path: 'tickets/new', component: TicketCreateComponent },
    { path: 'tickets/:id', component: TicketDetailComponent },
    { path: 'technician/queue', component: TechnicianQueueComponent, canActivate: [roleGuard('TECHNICIAN')] },
    { path: 'admin/users', component: AdminUsersComponent, canActivate: [roleGuard('ADMIN')] },
    { path: 'admin/categories', component: AdminCategoriesComponent, canActivate: [roleGuard('ADMIN')] },
    { path: 'admin/statistics', component: AdminStatisticsComponent, canActivate: [roleGuard('ADMIN')] },
    { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
  ] },
  { path: 'dashboard', pathMatch: 'full', redirectTo: 'app/dashboard' },
  { path: 'tickets', pathMatch: 'full', redirectTo: 'app/tickets' },
  { path: 'tickets/:id', pathMatch: 'full', redirectTo: 'app/tickets/:id' },
  { path: '', pathMatch: 'full', redirectTo: 'app/dashboard' },
  { path: '**', redirectTo: 'app/dashboard' }
];
