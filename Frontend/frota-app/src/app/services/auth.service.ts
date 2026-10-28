import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface AuthenticationResponse {
  token: string;
  username: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = 'http://localhost:8080/api';
  private readonly isBrowser: boolean;

  constructor(
    private readonly http: HttpClient,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  get token(): string | null {
    return this.isBrowser ? sessionStorage.getItem('frota.token') : null;
  }

  get username(): string | null {
    return this.isBrowser ? sessionStorage.getItem('frota.username') : null;
  }

  get roles(): string[] {
    const savedRoles = this.isBrowser ? sessionStorage.getItem('frota.roles') : null;
    return savedRoles ? savedRoles.split(',').filter(Boolean) : [];
  }

  get isAuthenticated(): boolean {
    return this.token !== null;
  }

  get isAdmin(): boolean {
    return this.hasAnyRole('ADMIN', 'SUPER_ADMIN');
  }

  login(username: string, password: string): Observable<AuthenticationResponse> {
    return this.http.post<AuthenticationResponse>(`${this.apiUrl}/auth/login`, { username, password }).pipe(
      tap(response => {
        if (!this.isBrowser) return;
        sessionStorage.setItem('frota.token', response.token);
        sessionStorage.setItem('frota.username', response.username);
        sessionStorage.setItem('frota.roles', response.roles.join(','));
      })
    );
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some(role => this.roles.includes(role));
  }

  logout(): void {
    if (!this.isBrowser) return;
    sessionStorage.removeItem('frota.token');
    sessionStorage.removeItem('frota.username');
    sessionStorage.removeItem('frota.roles');
  }
}
