import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="navbar navbar-expand-lg bg-dark navbar-dark">
      <div class="container">
        <span class="navbar-brand">HelpDesk empresarial</span>
        <button class="btn btn-outline-light btn-sm" type="button" (click)="logout()">Cerrar sesión</button>
      </div>
    </nav>

    <main class="container py-5">
      <div class="mb-4">
        <p class="text-body-secondary mb-1">Panel principal</p>
        <h1 class="h2">Hola, {{ authService.user()?.firstName }}</h1>
        <p class="mb-0">Has iniciado sesión como {{ roleLabel() }}.</p>
      </div>

      <div class="row g-4">
        <div class="col-md-4">
          <article class="card h-100 shadow-sm border-0">
            <div class="card-body">
              <h2 class="h5">Mis incidencias</h2>
              <p class="text-body-secondary">Aquí aparecerán tus tickets y su estado.</p>
              <button class="btn btn-primary btn-sm" type="button" disabled>Próximamente</button>
            </div>
          </article>
        </div>
        <div class="col-md-4">
          <article class="card h-100 shadow-sm border-0">
            <div class="card-body">
              <h2 class="h5">Actividad</h2>
              <p class="text-body-secondary">El historial de actividad estará disponible en la siguiente iteración.</p>
              <button class="btn btn-outline-secondary btn-sm" type="button" disabled>Próximamente</button>
            </div>
          </article>
        </div>
      </div>
    </main>
  `
})
export class DashboardComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  roleLabel(): string {
    const role = this.authService.user()?.roles[0];
    return role === 'ADMIN' ? 'administrador' : role === 'TECHNICIAN' ? 'técnico' : 'usuario';
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
