import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { finalize } from 'rxjs';
import { RouterLink } from '@angular/router';
import { TicketService } from '../../core/tickets/ticket.service';
import { Ticket, TicketStatus } from '../../core/tickets/ticket.models';
import { AuthService } from '../../core/auth/auth.service';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { PriorityBadgeComponent } from '../../shared/priority-badge.component';
import { apiErrorMessage } from '../../core/http/http-error';

@Component({ selector: 'app-dashboard', standalone: true, imports: [CommonModule, RouterLink, StatusBadgeComponent, PriorityBadgeComponent], template: `
  <section class="page-heading"><div><p class="eyebrow">{{ roleTitle }}</p><h1>{{ greeting }}</h1><p class="text-muted mb-0">Una vista rápida de las incidencias que requieren tu atención.</p></div><a class="btn btn-primary" routerLink="/app/tickets/new">＋ Nueva incidencia</a></section>
  <div *ngIf="loading" class="dashboard-grid" aria-busy="true"><div class="skeleton skeleton-card" *ngFor="let item of [1,2,3]"></div></div>
  <div *ngIf="error && !loading" class="state-card error-state" role="alert"><h2>No se ha podido cargar el panel</h2><p>{{ error }}</p><button class="btn btn-outline-primary" (click)="load()">Reintentar</button></div>
  <ng-container *ngIf="!loading && !error"><div class="dashboard-grid"><article class="metric-card"><span>Abiertas</span><strong>{{ count('OPEN') }}</strong><small>Incidencias nuevas</small></article><article class="metric-card"><span>En progreso</span><strong>{{ count('IN_PROGRESS') }}</strong><small>Trabajo activo</small></article><article class="metric-card"><span>Resueltas</span><strong>{{ count('RESOLVED') + count('CLOSED') }}</strong><small>Histórico resuelto</small></article></div>
    <section class="panel mt-4"><div class="panel-header"><div><h2>Incidencias recientes</h2><p class="text-muted mb-0">{{ tickets.length }} resultado{{ tickets.length === 1 ? '' : 's' }} autorizados</p></div><a routerLink="/app/tickets" class="btn btn-outline-primary btn-sm">Ver todas</a></div>
      <div *ngIf="tickets.length === 0" class="empty-state"><div class="empty-icon">✓</div><h3>No hay incidencias todavía</h3><p class="text-muted">Crea la primera incidencia para empezar a trabajar.</p><a routerLink="/app/tickets/new" class="btn btn-primary">Crear incidencia</a></div>
      <div *ngIf="tickets.length > 0" class="table-wrap"><table class="table align-middle mb-0"><thead><tr><th>ID</th><th>Incidencia</th><th>Estado</th><th>Prioridad</th><th>Actualizada</th></tr></thead><tbody><tr *ngFor="let ticket of recent"><td><a [routerLink]="['/app/tickets', ticket.id]" class="ticket-id">#{{ ticket.id }}</a></td><td><a [routerLink]="['/app/tickets', ticket.id]" class="ticket-title">{{ ticket.title }}</a><small class="d-block text-muted">{{ ticket.category.name }}</small></td><td><app-status-badge [status]="ticket.status" /></td><td><app-priority-badge [priority]="ticket.priority" /></td><td class="text-muted">{{ ticket.updatedAt | date:'dd/MM/yyyy HH:mm' }}</td></tr></tbody></table></div>
    </section></ng-container>
` })
export class DashboardComponent {
  private readonly service = inject(TicketService); private readonly cdr = inject(ChangeDetectorRef); readonly auth = inject(AuthService);
  tickets: Ticket[] = []; loading = true; error = '';
  get roleTitle(): string { return this.auth.hasRole('ADMIN') ? 'Panel global' : this.auth.hasRole('TECHNICIAN') ? 'Mi trabajo' : 'Mis incidencias'; }
  get greeting(): string { return `Hola, ${this.auth.user()?.firstName ?? 'usuario'}`; }
  get recent(): Ticket[] { return [...this.tickets].sort((a,b) => b.updatedAt.localeCompare(a.updatedAt)).slice(0, 5); }
  constructor() { this.load(); }
  load(): void { this.loading = true; this.error = ''; this.cdr.markForCheck(); this.service.list().pipe(finalize(() => { this.loading = false; this.cdr.markForCheck(); })).subscribe({ next: data => this.tickets = data, error: e => this.error = apiErrorMessage(e) }); }
  count(status: TicketStatus): number { return this.tickets.filter(ticket => ticket.status === status).length; }
}
