import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { AdminCategory, AdminUser, Attachment, Statistics, UpdateAdminUser } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  users(): Observable<AdminUser[]> { return this.http.get<AdminUser[]>(`${API_BASE_URL}/users`); }
  updateUser(id: number, request: UpdateAdminUser): Observable<AdminUser> { return this.http.patch<AdminUser>(`${API_BASE_URL}/users/${id}`, request); }
  categories(): Observable<AdminCategory[]> { return this.http.get<AdminCategory[]>(`${API_BASE_URL}/categories/manage`); }
  createCategory(name: string, description: string): Observable<AdminCategory> { return this.http.post<AdminCategory>(`${API_BASE_URL}/categories`, { name, description }); }
  updateCategory(id: number, name: string, description: string): Observable<AdminCategory> { return this.http.patch<AdminCategory>(`${API_BASE_URL}/categories/${id}`, { name, description }); }
  setCategoryActive(id: number, active: boolean): Observable<AdminCategory> { return this.http.patch<AdminCategory>(`${API_BASE_URL}/categories/${id}/active`, { active }); }
  statistics(days = 30): Observable<Statistics> { return this.http.get<Statistics>(`${API_BASE_URL}/statistics`, { params: { days } }); }
  attachments(ticketId: number): Observable<Attachment[]> { return this.http.get<Attachment[]>(`${API_BASE_URL}/tickets/${ticketId}/attachments`); }
  uploadAttachment(ticketId: number, file: File): Observable<Attachment> { const form = new FormData(); form.append('file', file); return this.http.post<Attachment>(`${API_BASE_URL}/tickets/${ticketId}/attachments`, form); }
  downloadAttachment(ticketId: number, attachmentId: number): Observable<Blob> { return this.http.get(`${API_BASE_URL}/tickets/${ticketId}/attachments/${attachmentId}/download`, { responseType: 'blob' }); }
}
