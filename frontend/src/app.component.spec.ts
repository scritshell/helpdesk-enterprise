import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  it('crea el componente raíz', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();

    expect(fixture.componentInstance).toBeInstanceOf(AppComponent);
  });

  it('muestra el título y el texto inicial de HelpDesk', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h1')?.textContent).toContain('HelpDesk empresarial');
    expect(element.textContent).toContain('Base del frontend preparada para el módulo de autenticación.');
  });
});
