import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { TicketService } from '../../core/tickets/ticket.service';
import { Ticket } from '../../core/tickets/ticket.models';
import { apiErrorMessage } from '../../core/http/http-error';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { PriorityBadgeComponent } from '../../shared/priority-badge.component';

@Component({ selector: 'app-technician-queue', standalone: true, imports: [CommonModule, RouterLink, StatusBadgeComponent, PriorityBadgeComponent], template: `
  <section class="page-heading"><div><p class="eyebrow">Soporte técnico</p><h1>Cola de trabajo</h1><p class="text-muted mb-0">Incidencias creadas por usuarios que todavía no tienen técnico asignado.</p></div><a routerLink="/app/tickets" class="btn btn-outline-primary">Mis incidencias</a></section>
  <div *ngIf="loading" class="panel skeleton-table" aria-busy="true"><div class="skeleton skeleton-line" *ngFor="let row of [1,2,3,4]"></div></div>
  <div *ngIf="error && !loading" class="state-card error-state" role="alert"><h2>No se ha podido cargar la cola</h2><p>{{ error }}</p><button class="btn btn-outline-primary" (click)="load()">Reintentar</button></div>
  <section *ngIf="!loading && !error" class="panel"><div *ngIf="tickets.length === 0" class="empty-state"><div class="empty-icon">✓</div><h2>Cola vacía</h2><p class="text-muted">No hay incidencias sin asignar en este momento.</p></div><div *ngIf="tickets.length > 0" class="table-wrap"><table class="table align-middle mb-0"><thead><tr><th>ID</th><th>Incidencia</th><th>Estado</th><th>Prioridad</th><th>Creada</th><th></th></tr></thead><tbody><tr *ngFor="let ticket of tickets"><td class="ticket-id">#{{ ticket.id }}</td><td><strong class="ticket-title">{{ ticket.title }}</strong><small class="d-block text-muted">{{ ticket.category.name }}</small></td><td><app-status-badge [status]="ticket.status" /></td><td><app-priority-badge [priority]="ticket.priority" /></td><td class="text-muted">{{ ticket.createdAt | date:'dd/MM/yyyy HH:mm' }}</td><td><button class="btn btn-sm btn-primary" [disabled]="claimingId === ticket.id" (click)="claim(ticket)">{{ claimingId === ticket.id ? 'Reclamando…' : 'Reclamar' }}</button></td></tr></tbody></table></div></section>
` })
export class TechnicianQueueComponent {
  private readonly service = inject(TicketService); private readonly cdr = inject(ChangeDetectorRef);
  tickets: Ticket[] = []; loading = true; error = ''; claimingId: number | null = null;
  constructor() { this.load(); }
  load(): void { this.loading = true; this.error = ''; this.service.queue().pipe(finalize(() => { this.loading = false; this.cdr.markForCheck(); })).subscribe({ next: tickets => this.tickets = tickets, error: error => this.error = apiErrorMessage(error) }); }
  claim(ticket: Ticket): void { this.claimingId = ticket.id; this.service.claim(ticket.id).pipe(finalize(() => { this.claimingId = null; this.cdr.markForCheck(); })).subscribe({ next: claimed => { this.tickets = this.tickets.filter(item => item.id !== claimed.id); }, error: error => this.error = apiErrorMessage(error, 'No se ha podido reclamar la incidencia.') }); }
}
