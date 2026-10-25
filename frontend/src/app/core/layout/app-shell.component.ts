import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-shell', standalone: true, imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-shell" [class.sidebar-collapsed]="collapsed">
      <aside class="sidebar" aria-label="Navegación principal">
        <div class="brand"><span class="brand-mark">H</span><span *ngIf="!collapsed">HelpDesk</span></div>
        <div *ngIf="!collapsed" class="sidebar-role">{{ roleLabel }}</div>
        <nav class="nav flex-column gap-1">
          <a routerLink="/app/dashboard" routerLinkActive="active" class="nav-link"><span aria-hidden="true">⌂</span><span *ngIf="!collapsed">Panel</span></a>
          <a routerLink="/app/tickets" routerLinkActive="active" class="nav-link"><span aria-hidden="true">▤</span><span *ngIf="!collapsed">{{ ticketsLabel }}</span></a>
          <a *ngIf="isTechnician" routerLink="/app/technician/queue" routerLinkActive="active" class="nav-link"><span aria-hidden="true">◈</span><span *ngIf="!collapsed">Cola de trabajo</span></a>
          <a *ngIf="isAdmin" routerLink="/app/admin/users" routerLinkActive="active" class="nav-link"><span aria-hidden="true">♙</span><span *ngIf="!collapsed">Usuarios y roles</span></a>
          <a *ngIf="isAdmin" routerLink="/app/admin/categories" routerLinkActive="active" class="nav-link"><span aria-hidden="true">▦</span><span *ngIf="!collapsed">Categorías</span></a>
          <a *ngIf="isAdmin" routerLink="/app/admin/statistics" routerLinkActive="active" class="nav-link"><span aria-hidden="true">▥</span><span *ngIf="!collapsed">Estadísticas</span></a>
          <a *ngIf="canCreate" routerLink="/app/tickets/new" routerLinkActive="active" class="nav-link"><span aria-hidden="true">＋</span><span *ngIf="!collapsed">Nueva incidencia</span></a>
        </nav>
        <div class="sidebar-footer" *ngIf="!collapsed"><span class="small text-muted">Sesión activa</span><span class="small">{{ userName }}</span></div>
      </aside>
      <div class="main-area">
        <header class="topbar">
          <button class="icon-button" type="button" (click)="toggleSidebar()" [attr.aria-label]="collapsed ? 'Expandir menú' : 'Colapsar menú'">☰</button>
          <div class="breadcrumb-text">{{ pageTitle }}</div>
          <div class="topbar-actions"><span class="avatar" aria-hidden="true">{{ initials }}</span><span class="user-name">{{ userName }}</span><button class="btn btn-sm btn-outline-secondary" (click)="logout()">Salir</button></div>
        </header>
        <main class="page-content"><router-outlet /></main>
      </div>
    </div>
  `
})
export class AppShellComponent {
  readonly auth = inject(AuthService); private readonly router = inject(Router);
  collapsed = false;
  get userName(): string { const u = this.auth.user(); return u ? `${u.firstName} ${u.lastName}` : 'Usuario'; }
  get initials(): string { const u = this.auth.user(); return u ? `${u.firstName[0] ?? ''}${u.lastName[0] ?? ''}`.toUpperCase() : '?'; }
  get canCreate(): boolean { return this.auth.hasRole('USER') || this.auth.hasRole('TECHNICIAN') || this.auth.hasRole('ADMIN'); }
  get roleLabel(): string { return this.auth.hasRole('ADMIN') ? 'ADMINISTRACIÓN' : this.auth.hasRole('TECHNICIAN') ? 'SOPORTE TÉCNICO' : 'ESPACIO DE USUARIO'; }
  get ticketsLabel(): string { return this.auth.hasRole('ADMIN') ? 'Todas las incidencias' : this.auth.hasRole('TECHNICIAN') ? 'Asignadas a mí' : 'Mis incidencias'; }
  get isTechnician(): boolean { return this.auth.hasRole('TECHNICIAN'); }
  get isAdmin(): boolean { return this.auth.hasRole('ADMIN'); }
  get pageTitle(): string { return this.router.url.includes('/tickets') ? 'Incidencias' : 'Panel principal'; }
  toggleSidebar(): void { this.collapsed = !this.collapsed; }
  logout(): void { this.auth.logout(); void this.router.navigate(['/login']); }
}
