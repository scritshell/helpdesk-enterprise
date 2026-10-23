import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(error: unknown, fallback = 'No se ha podido completar la operación.'): string {
  if (!(error instanceof HttpErrorResponse)) return fallback;
  if (error.status === 401) return 'Tu sesión ha caducado. Vuelve a iniciar sesión.';
  if (error.status === 403) return 'No tienes permisos para realizar esta acción.';
  if (error.status === 404) return 'La incidencia o el recurso solicitado no existe.';
  if (error.status === 409) return 'La incidencia ha cambiado o la transición no es válida. Recarga los datos e inténtalo de nuevo.';
  if (error.status === 0) return 'No se puede conectar con el servidor. Comprueba que el backend está disponible.';
  return fallback;
}
