import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';
import { RoleName } from './auth.models';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

export const roleGuard = (role: RoleName): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.hasRole(role) ? true : router.createUrlTree(['/dashboard']);
};
