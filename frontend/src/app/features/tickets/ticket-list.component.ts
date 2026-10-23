import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs';
import { CategoryService, TicketService } from '../../core/tickets/ticket.service';
import { Category, PRIORITY_LABELS, PRIORITY_VALUES, STATUS_LABELS, STATUS_VALUES, Ticket, TicketPriority, TicketStatus } from '../../core/tickets/ticket.models';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { PriorityBadgeComponent } from '../../shared/priority-badge.component';
import { apiErrorMessage } from '../../core/http/http-error';

@Component({ selector: 'app-ticket-list', standalone: true, imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent, PriorityBadgeComponent], template: `
  <section class="page-heading"><div><p class="eyebrow">Trabajo</p><h1>Incidencias</h1><p class="text-muted mb-0">Consulta y encuentra las incidencias que tienes autorizadas.</p></div><a routerLink="/app/tickets/new" class="btn btn-primary">＋ Nueva incidencia</a></section>
  <div *ngIf="loading" class="panel skeleton-table" aria-busy="true"><div class="skeleton skeleton-line" *ngFor="let row of [1,2,3,4,5]"></div></div>
  <div *ngIf="error && !loading" class="state-card error-state" role="alert"><h2>No se han podido cargar las incidencias</h2><p>{{ error }}</p><button class="btn btn-outline-primary" (click)="load()">Reintentar</button></div>
  <section *ngIf="!loading && !error" class="panel"><div class="filters"><div class="filter-search"><label for="search">Buscar</label><input id="search" class="form-control" [(ngModel)]="search" (ngModelChange)="resetPage()" placeholder="Título o descripción" /></div><div><label for="status">Estado</label><select id="status" class="form-select" [(ngModel)]="status" (ngModelChange)="resetPage()"><option value="">Todos</option><option *ngFor="let item of statusValues" [value]="item">{{ statusLabels[item] }}</option></select></div><div><label for="priority">Prioridad</label><select id="priority" class="form-select" [(ngModel)]="priority" (ngModelChange)="resetPage()"><option value="">Todas</option><option *ngFor="let item of priorityValues" [value]="item">{{ priorityLabels[item] }}</option></select></div><div><label for="category">Categoría</label><select id="category" class="form-select" [(ngModel)]="categoryId" (ngModelChange)="resetPage()"><option [ngValue]="null">Todas</option><option *ngFor="let item of categories" [ngValue]="item.id">{{ item.name }}</option></select></div><div><label for="sort">Ordenar</label><select id="sort" class="form-select" [(ngModel)]="sort" (ngModelChange)="resetPage()"><option value="updated">Actualización</option><option value="priority">Prioridad</option><option value="title">Título</option></select></div><button class="btn btn-link align-self-end" (click)="clearFilters()">Limpiar</button></div>
    <div class="panel-header"><p class="text-muted mb-0">{{ filtered.length }} resultado{{ filtered.length === 1 ? '' : 's' }}</p><span class="small text-muted">Página {{ page }} de {{ totalPages }}</span></div>
    <div *ngIf="filtered.length === 0" class="empty-state"><div class="empty-icon">⌕</div><h2>No hay coincidencias</h2><p class="text-muted">Prueba a cambiar los filtros o crea una nueva incidencia.</p><button class="btn btn-outline-primary" (click)="clearFilters()">Limpiar filtros</button></div>
    <div *ngIf="filtered.length > 0" class="table-wrap"><table class="table align-middle mb-0"><thead><tr><th>ID</th><th>Incidencia</th><th>Estado</th><th>Prioridad</th><th>Categoría</th><th>Asignado</th><th>Actualizada</th></tr></thead><tbody><tr *ngFor="let ticket of paged"><td><a class="ticket-id" [routerLink]="['/app/tickets', ticket.id]">#{{ ticket.id }}</a></td><td><a class="ticket-title" [routerLink]="['/app/tickets', ticket.id]">{{ ticket.title }}</a><small class="d-block text-muted text-truncate cell-description">{{ ticket.description }}</small></td><td><app-status-badge [status]="ticket.status" /></td><td><app-priority-badge [priority]="ticket.priority" /></td><td>{{ ticket.category.name }}</td><td>{{ ticket.technician ? ticket.technician.firstName + ' ' + ticket.technician.lastName : 'Sin asignar' }}</td><td class="text-muted">{{ ticket.updatedAt | date:'dd/MM/yyyy HH:mm' }}</td></tr></tbody></table></div>
    <nav *ngIf="filtered.length > 0" class="pagination-bar" aria-label="Paginación"><button class="btn btn-sm btn-outline-secondary" [disabled]="page === 1" (click)="page = page - 1">Anterior</button><span>{{ page }} / {{ totalPages }}</span><button class="btn btn-sm btn-outline-secondary" [disabled]="page === totalPages" (click)="page = page + 1">Siguiente</button></nav>
  </section>
` })
export class TicketListComponent {
  private readonly ticketsService = inject(TicketService); private readonly categoryService = inject(CategoryService); private readonly cdr = inject(ChangeDetectorRef);
  tickets: Ticket[] = []; categories: Category[] = []; loading = true; error = ''; search = ''; status: TicketStatus | '' = ''; priority: TicketPriority | '' = ''; categoryId: number | null = null; sort = 'updated'; page = 1; readonly pageSize = 8;
  readonly statusValues = STATUS_VALUES; readonly priorityValues = PRIORITY_VALUES; readonly statusLabels = STATUS_LABELS; readonly priorityLabels = PRIORITY_LABELS;
  constructor() { this.load(); }
  load(): void { this.loading = true; this.error = ''; this.cdr.markForCheck(); forkJoin({ tickets: this.ticketsService.list(), categories: this.categoryService.list() }).pipe(finalize(() => { this.loading = false; this.cdr.markForCheck(); })).subscribe({ next: value => { this.tickets = value.tickets; this.categories = value.categories; }, error: e => this.error = apiErrorMessage(e) }); }
  get filtered(): Ticket[] { const query = this.search.trim().toLocaleLowerCase(); return this.tickets.filter(ticket => (!query || `${ticket.title} ${ticket.description}`.toLocaleLowerCase().includes(query)) && (!this.status || ticket.status === this.status) && (!this.priority || ticket.priority === this.priority) && (this.categoryId === null || ticket.category.id === this.categoryId)).sort((a,b) => this.compare(a,b)); }
  get paged(): Ticket[] { const start = (this.page - 1) * this.pageSize; return this.filtered.slice(start, start + this.pageSize); }
  get totalPages(): number { return Math.max(1, Math.ceil(this.filtered.length / this.pageSize)); }
  compare(a: Ticket, b: Ticket): number { if (this.sort === 'title') return a.title.localeCompare(b.title); if (this.sort === 'priority') return PRIORITY_VALUES.indexOf(a.priority) - PRIORITY_VALUES.indexOf(b.priority); return b.updatedAt.localeCompare(a.updatedAt); }
  resetPage(): void { this.page = 1; }
  clearFilters(): void { this.search = ''; this.status = ''; this.priority = ''; this.categoryId = null; this.sort = 'updated'; this.resetPage(); }
}
