import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CategoryService, TicketService } from '../../core/tickets/ticket.service';
import { Category, PRIORITY_LABELS, PRIORITY_VALUES, TicketPriority } from '../../core/tickets/ticket.models';
import { apiErrorMessage } from '../../core/http/http-error';

@Component({ selector: 'app-ticket-create', standalone: true, imports: [CommonModule, ReactiveFormsModule, RouterLink], template: `
  <section class="page-heading"><div><p class="eyebrow">Nueva incidencia</p><h1>Cuéntanos qué ocurre</h1><p class="text-muted mb-0">Incluye los datos necesarios para que soporte pueda ayudarte.</p></div></section>
  <div *ngIf="loadingCategories" class="panel skeleton-form" aria-busy="true"><div class="skeleton skeleton-line" *ngFor="let row of [1,2,3]"></div></div>
  <div *ngIf="categoryError" class="state-card error-state" role="alert"><h2>No se pueden cargar las categorías</h2><p>{{ categoryError }}</p><button class="btn btn-outline-primary" (click)="loadCategories()">Reintentar</button></div>
  <form *ngIf="!loadingCategories && !categoryError" class="panel form-panel" [formGroup]="form" (ngSubmit)="submit()" novalidate>
    <div *ngIf="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <div class="form-main"><div class="mb-4"><label for="title" class="form-label">Título</label><input id="title" class="form-control" formControlName="title" maxlength="160" [attr.aria-invalid]="invalid('title')" /><div *ngIf="invalid('title')" class="field-error">Escribe un título de hasta 160 caracteres.</div></div><div><label for="description" class="form-label">Descripción</label><textarea id="description" class="form-control" rows="9" formControlName="description" [attr.aria-invalid]="invalid('description')"></textarea><div *ngIf="invalid('description')" class="field-error">La descripción es obligatoria.</div></div></div>
    <div class="form-side"><div class="mb-4"><label for="category" class="form-label">Categoría</label><select id="category" class="form-select" formControlName="categoryId"><option [ngValue]="null">Selecciona una categoría</option><option *ngFor="let category of categories" [ngValue]="category.id">{{ category.name }}</option></select><div *ngIf="invalid('categoryId')" class="field-error">Selecciona una categoría.</div></div><div><label for="priority" class="form-label">Prioridad</label><select id="priority" class="form-select" formControlName="priority"><option *ngFor="let item of priorities" [value]="item">{{ priorityLabels[item] }}</option></select><p class="form-hint">La prioridad indica la urgencia, no el estado del trabajo.</p></div></div>
    <div class="form-actions"><a routerLink="/app/tickets" class="btn btn-outline-secondary">Cancelar</a><button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving"><span *ngIf="saving" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>{{ saving ? 'Creando…' : 'Crear incidencia' }}</button></div>
  </form>
` })
export class TicketCreateComponent {
  private readonly fb = inject(FormBuilder); private readonly categoriesService = inject(CategoryService); private readonly ticketsService = inject(TicketService); private readonly router = inject(Router); private readonly cdr = inject(ChangeDetectorRef);
  readonly form = this.fb.nonNullable.group({ title: ['', [Validators.required, Validators.maxLength(160)]], description: ['', Validators.required], categoryId: [null as number | null, Validators.required], priority: ['MEDIUM' as TicketPriority, Validators.required] });
  categories: Category[] = []; priorities = PRIORITY_VALUES; readonly priorityLabels = PRIORITY_LABELS; loadingCategories = true; saving = false; categoryError = ''; error = '';
  constructor() { this.loadCategories(); }
  loadCategories(): void { this.loadingCategories = true; this.categoryError = ''; this.cdr.markForCheck(); this.categoriesService.list().pipe(finalize(() => { this.loadingCategories = false; this.cdr.markForCheck(); })).subscribe({ next: data => this.categories = data, error: e => this.categoryError = apiErrorMessage(e) }); }
  invalid(name: 'title' | 'description' | 'categoryId'): boolean { const control = this.form.controls[name]; return control.invalid && (control.dirty || control.touched); }
  submit(): void { this.error = ''; if (this.form.invalid) { this.form.markAllAsTouched(); return; } this.saving = true; this.cdr.markForCheck(); this.ticketsService.create(this.form.getRawValue() as { title: string; description: string; categoryId: number; priority: TicketPriority }).pipe(finalize(() => { this.saving = false; this.cdr.markForCheck(); })).subscribe({ next: ticket => void this.router.navigate(['/app/tickets', ticket.id]), error: e => this.error = apiErrorMessage(e, 'No se ha podido crear la incidencia.') }); }
}
