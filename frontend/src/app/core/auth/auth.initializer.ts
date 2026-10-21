import { inject } from '@angular/core';
import { catchError, of } from 'rxjs';

import { AuthService } from './auth.service';

export function initializeAuthSession() {
  const authService = inject(AuthService);
  return authService.isAuthenticated()
    ? authService.loadCurrentUser().pipe(catchError(() => {
        authService.clearSession();
        return of(null);
      }))
    : of(null);
}
