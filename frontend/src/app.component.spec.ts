import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  it('crea el componente raíz con el outlet de navegación', async () => {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])]
    });

    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();

    expect(fixture.componentInstance).toBeInstanceOf(AppComponent);
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });
});
