import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { apiErrorMessage } from './http-error';

describe('apiErrorMessage', () => {
  it('traduce los conflictos de transición y los permisos', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 403 }))).toContain('permisos');
    expect(apiErrorMessage(new HttpErrorResponse({ status: 409 }))).toContain('transición');
    expect(apiErrorMessage(new HttpErrorResponse({ status: 404 }))).toContain('no existe');
  });
});
