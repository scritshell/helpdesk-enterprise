import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <main class="login-page d-flex align-items-center min-vh-100 bg-light py-5">
      <section class="container" aria-labelledby="login-title">
        <div class="row justify-content-center">
          <div class="col-12 col-sm-10 col-md-7 col-lg-5 col-xl-4">
            <div class="card border-0 shadow-sm">
              <div class="card-body p-4 p-md-5">
                <div class="text-center mb-4">
                  <span class="badge text-bg-primary mb-3">HelpDesk</span>
                  <h1 id="login-title" class="h3 mb-2">Iniciar sesión</h1>
                  <p class="text-body-secondary mb-0">Accede a tu espacio de trabajo.</p>
                </div>

                <div *ngIf="errorMessage" class="alert alert-danger" role="alert">
                  {{ errorMessage }}
                </div>

                <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
                  <div class="mb-3">
                    <label for="email" class="form-label">Correo electrónico</label>
                    <input
                      id="email"
                      type="email"
                      class="form-control"
                      formControlName="email"
                      autocomplete="username"
                      inputmode="email"
                      spellcheck="false"
                      [attr.aria-invalid]="isInvalid('email')"
                    />
                    <div *ngIf="isInvalid('email')" class="invalid-feedback d-block">
                      Introduce un correo electrónico válido.
                    </div>
                  </div>

                  <div class="mb-4">
                    <label for="password" class="form-label">Contraseña</label>
                    <input
                      id="password"
                      type="password"
                      class="form-control"
                      formControlName="password"
                      autocomplete="current-password"
                      [attr.aria-invalid]="isInvalid('password')"
                    />
                    <div *ngIf="isInvalid('password')" class="invalid-feedback d-block">
                      La contraseña es obligatoria.
                    </div>
                  </div>

                  <button class="btn btn-primary w-100" type="submit" [disabled]="form.invalid || loading">
                    <span *ngIf="loading" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
                    {{ loading ? 'Comprobando…' : 'Entrar' }}
                  </button>
                </form>

                <p class="small text-body-secondary text-center mt-4 mb-0">
                  Las credenciales se envían únicamente al endpoint seguro de autenticación.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  `
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  loading = false;
  errorMessage = '';

  isInvalid(controlName: 'email' | 'password'): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  submit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading = false;
        void this.router.navigate(['/app/dashboard']);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No se ha podido iniciar sesión. Comprueba tus credenciales e inténtalo de nuevo.';
      }
    });
  }
}
