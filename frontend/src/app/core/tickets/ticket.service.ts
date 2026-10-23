import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { Category, CreateTicketRequest, Technician, Ticket, TicketComment, TicketHistory, TicketPriority, TicketStatus } from './ticket.models';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/tickets`;
  list(): Observable<Ticket[]> { return this.http.get<Ticket[]>(this.url); }
  queue(): Observable<Ticket[]> { return this.http.get<Ticket[]>(`${this.url}/queue`); }
  get(id: number): Observable<Ticket> { return this.http.get<Ticket>(`${this.url}/${id}`); }
  create(request: CreateTicketRequest): Observable<Ticket> { return this.http.post<Ticket>(this.url, request); }
  comments(id: number): Observable<TicketComment[]> { return this.http.get<TicketComment[]>(`${this.url}/${id}/comments`); }
  history(id: number): Observable<TicketHistory[]> { return this.http.get<TicketHistory[]>(`${this.url}/${id}/history`); }
  addComment(id: number, body: string): Observable<TicketComment> { return this.http.post<TicketComment>(`${this.url}/${id}/comments`, { body }); }
  changeStatus(id: number, status: TicketStatus): Observable<Ticket> { return this.http.patch<Ticket>(`${this.url}/${id}/status`, { status }); }
  changePriority(id: number, priority: TicketPriority): Observable<Ticket> { return this.http.patch<Ticket>(`${this.url}/${id}/priority`, { priority }); }
  assign(id: number, technicianId: number): Observable<Ticket> { return this.http.patch<Ticket>(`${this.url}/${id}/assignment`, { technicianId }); }
  claim(id: number): Observable<Ticket> { return this.http.post<Ticket>(`${this.url}/${id}/claim`, {}); }
}

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  list(): Observable<Category[]> { return this.http.get<Category[]>(`${API_BASE_URL}/categories`); }
}

@Injectable({ providedIn: 'root' })
export class UserDirectoryService {
  private readonly http = inject(HttpClient);
  technicians(): Observable<Technician[]> { return this.http.get<Technician[]>(`${API_BASE_URL}/users/technicians`); }
}
