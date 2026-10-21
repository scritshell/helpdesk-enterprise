import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { LoginRequest, LoginResponse, RoleName, UserResponse } from './auth.models';

const SESSION_TOKEN_KEY = 'helpdesk.session.token';
const SESSION_USER_KEY = 'helpdesk.session.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userState = signal<UserResponse | null>(this.readStoredUser());

  readonly user = this.userState.asReadonly();

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/login`, request).pipe(
      tap(response => this.storeSession(response))
    );
  }

  loadCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${API_BASE_URL}/auth/me`).pipe(
      tap(user => {
        this.userState.set(user);
        this.storeUser(user);
      })
    );
  }

  logout(): void {
    sessionStorage.removeItem(SESSION_TOKEN_KEY);
    sessionStorage.removeItem(SESSION_USER_KEY);
    this.userState.set(null);
  }

  clearSession(): void {
    this.logout();
  }

  getToken(): string | null {
    return sessionStorage.getItem(SESSION_TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  hasRole(role: RoleName): boolean {
    return this.userState()?.roles.includes(role) ?? false;
  }

  private storeSession(response: LoginResponse): void {
    sessionStorage.setItem(SESSION_TOKEN_KEY, response.token);
    this.storeUser(response.user);
    this.userState.set(response.user);
  }

  private storeUser(user: UserResponse): void {
    sessionStorage.setItem(SESSION_USER_KEY, JSON.stringify(user));
  }

  private readStoredUser(): UserResponse | null {
    const rawUser = sessionStorage.getItem(SESSION_USER_KEY);
    if (!rawUser) return null;

    try {
      return JSON.parse(rawUser) as UserResponse;
    } catch {
      sessionStorage.removeItem(SESSION_USER_KEY);
      return null;
    }
  }
}
