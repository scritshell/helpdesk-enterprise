import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AdminService } from '../../core/admin/admin.service';
import { CountItem, Statistics } from '../../core/admin/admin.models';
import { apiErrorMessage } from '../../core/http/http-error';
import { PRIORITY_LABELS, TicketPriority } from '../../core/tickets/ticket.models';

@Component({ selector: 'app-admin-statistics', standalone: true, imports: [CommonModule, FormsModule], template: `
  <section class="page-heading"><div><p class="eyebrow">Administración</p><h1>Estadísticas</h1><p class="text-muted mb-0">Datos agregados del backend durante los últimos {{ days }} días.</p></div><select class="form-select w-auto" [(ngModel)]="days" (ngModelChange)="load()" aria-label="Periodo"><option [ngValue]="7">7 días</option><option [ngValue]="30">30 días</option><option [ngValue]="90">90 días</option></select></section>
  <div *ngIf="loading" class="dashboard-grid" aria-busy="true"><div class="skeleton skeleton-card" *ngFor="let item of [1,2,3,4]"></div></div><div *ngIf="error && !loading" class="state-card error-state" role="alert"><h2>No se han podido cargar las estadísticas</h2><p>{{ error }}</p><button class="btn btn-outline-primary" (click)="load()">Reintentar</button></div>
  <ng-container *ngIf="stats && !loading && !error"><div class="dashboard-grid"><article class="metric-card"><span>Total</span><strong>{{ stats.total }}</strong><small>Incidencias registradas</small></article><article class="metric-card"><span>Abiertas</span><strong>{{ stats.open }}</strong><small>OPEN + IN_PROGRESS</small></article><article class="metric-card"><span>Pendientes</span><strong>{{ stats.pending }}</strong><small>Esperando al usuario</small></article><article class="metric-card"><span>Resueltas / cerradas</span><strong>{{ stats.resolved + stats.closed }}</strong><small>{{ stats.resolved }} resueltas · {{ stats.closed }} cerradas</small></article></div><div class="stats-columns mt-4"><section class="panel"><h2>Por prioridad</h2><div class="bar-list"><div *ngFor="let item of stats.priorities" class="bar-row"><div class="bar-label"><span>{{ priorityLabel(item.label) }}</span><strong>{{ item.value }}</strong></div><div class="bar-track"><span class="bar-fill priority-fill" [style.width.%]="barWidth(item, stats.priorities)"></span></div></div></div></section><section class="panel"><h2>Por categoría</h2><div class="bar-list"><div *ngFor="let item of stats.categories" class="bar-row"><div class="bar-label"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div><div class="bar-track"><span class="bar-fill category-fill" [style.width.%]="barWidth(item, stats.categories)"></span></div></div><div *ngIf="stats.categories.length === 0" class="empty-inline">No hay datos de categorías.</div></div></section></div><section class="panel mt-4"><div class="panel-header"><div><h2>Evolución</h2><p class="text-muted mb-0">Incidencias creadas y transiciones a resuelta/cerrada.</p></div></div><div class="table-wrap"><table class="table align-middle mb-0"><thead><tr><th>Fecha</th><th>Creadas</th><th>Resueltas</th><th>Cerradas</th></tr></thead><tbody><tr *ngFor="let day of stats.evolution"><td>{{ day.date | date:'dd/MM/yyyy' }}</td><td>{{ day.created }}</td><td>{{ day.resolved }}</td><td>{{ day.closed }}</td></tr></tbody></table></div></section></ng-container>
` })
export class AdminStatisticsComponent {
  private readonly service = inject(AdminService); private readonly cdr = inject(ChangeDetectorRef);
  stats: Statistics | null = null; days = 30; loading = true; error = ''; readonly priorityLabels = PRIORITY_LABELS;
  constructor() { this.load(); }
  load(): void { this.loading = true; this.error = ''; this.service.statistics(this.days).pipe(finalize(() => { this.loading = false; this.cdr.markForCheck(); })).subscribe({ next: data => this.stats = data, error: error => this.error = apiErrorMessage(error, 'No se han podido cargar las estadísticas.') }); }
  barWidth(item: CountItem, values: CountItem[]): number { const max = Math.max(...values.map(value => value.value), 1); return item.value / max * 100; }
  priorityLabel(label: string): string { return this.priorityLabels[label as TicketPriority] ?? label; }
}
