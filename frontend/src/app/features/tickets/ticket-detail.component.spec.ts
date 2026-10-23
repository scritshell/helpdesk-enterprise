import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { Ticket, TicketComment } from '../../core/tickets/ticket.models';
import { TicketService, UserDirectoryService } from '../../core/tickets/ticket.service';
import { TicketDetailComponent } from './ticket-detail.component';

const ticket = {
  id: 1, title: 'Correo', description: 'No funciona', status: 'RESOLVED', priority: 'HIGH',
  category: { id: 1, name: 'Correo' },
  reporter: { id: 1, email: 'user@test.local', firstName: 'User', lastName: 'Test' },
  technician: { id: 2, email: 'tech@test.local', firstName: 'Tech', lastName: 'Test' },
  createdAt: '2026-01-01', updatedAt: '2026-01-02'
} as Ticket;
const comment: TicketComment = { id: 8, body: 'Actualizado', author: ticket.reporter, createdAt: '2026-01-03' };

describe('TicketDetailComponent', () => {
  function setup(commentRequest: Subject<TicketComment>) {
    const ticketService = {
      get: vi.fn(() => of(ticket)), comments: vi.fn(() => of([])), history: vi.fn(() => of([])),
      addComment: vi.fn(() => commentRequest), changeStatus: vi.fn(), changePriority: vi.fn(), assign: vi.fn(), claim: vi.fn()
    };
    TestBed.configureTestingModule({
      imports: [TicketDetailComponent], providers: [provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } },
        { provide: AuthService, useValue: { user: () => ticket.technician, hasRole: (role: string) => role === 'TECHNICIAN' } },
        { provide: TicketService, useValue: ticketService }, { provide: UserDirectoryService, useValue: { technicians: () => of([ticket.technician]) } }
      ]
    });
    return { fixture: TestBed.createComponent(TicketDetailComponent), ticketService };
  }

  it('termina la carga inicial aunque las tres peticiones sean asíncronas', async () => {
    const { fixture } = setup(new Subject<TicketComment>());
    await fixture.whenStable();
    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.componentInstance.error).toBe('');
  });

  it('libera el estado de envío y añade el comentario cuando la petición termina', async () => {
    const request = new Subject<TicketComment>();
    const { fixture } = setup(request);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    component.commentBody = 'Actualizado';
    component.addComment();
    expect(component.commentSaving).toBe(true);
    request.next(comment);
    request.complete();
    await fixture.whenStable();
    expect(component.commentSaving).toBe(false);
    expect(component.comments).toEqual([comment]);
  });
});
