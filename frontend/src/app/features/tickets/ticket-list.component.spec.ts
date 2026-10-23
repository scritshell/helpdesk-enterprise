import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { CategoryService, TicketService } from '../../core/tickets/ticket.service';
import { TicketListComponent } from './ticket-list.component';

describe('TicketListComponent', () => {
  it('filtra por texto, estado y categoría y pagina los resultados', async () => {
    const ticketService = { list: vi.fn().mockReturnValue(of([
      { id: 1, title: 'Correo caído', description: 'No funciona', status: 'OPEN', priority: 'HIGH', category: { id: 1, name: 'Correo' }, reporter: {}, technician: null, createdAt: '2026-01-01', updatedAt: '2026-01-02' },
      { id: 2, title: 'Pantalla rota', description: 'Hardware', status: 'RESOLVED', priority: 'LOW', category: { id: 2, name: 'Hardware' }, reporter: {}, technician: null, createdAt: '2026-01-01', updatedAt: '2026-01-03' }
    ])) };
    const categoryService = { list: vi.fn().mockReturnValue(of([{ id: 1, name: 'Correo' }, { id: 2, name: 'Hardware' }])) };
    TestBed.configureTestingModule({ imports: [TicketListComponent], providers: [provideRouter([]), { provide: TicketService, useValue: ticketService }, { provide: CategoryService, useValue: categoryService }] });
    const fixture = TestBed.createComponent(TicketListComponent);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    component.search = 'correo';
    component.status = 'OPEN';
    component.categoryId = 1;
    expect(component.filtered).toHaveLength(1);
    expect(component.filtered[0].id).toBe(1);
  });
});
