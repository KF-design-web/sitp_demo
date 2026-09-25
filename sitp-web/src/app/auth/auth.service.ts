import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_URL } from '../core/api-url';
import { AuthResponse, UserResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  register(body: {
    email: string;
    password: string;
    accountType: 'INTERN' | 'OUTSIDER';
    track?: string | null;
  }) {
    let params = new HttpParams()
      .set('email', body.email)
      .set('password', body.password)
      .set('accountType', body.accountType);
    if (body.track) {
      params = params.set('track', body.track);
    }
    return this.http.post<UserResponse>(`${API_URL}/auth/register`, params);
  }

  login(body: { email: string; password: string }) {
    const params = new HttpParams()
      .set('email', body.email)
      .set('password', body.password);
    return this.http.post<AuthResponse>(`${API_URL}/auth/login`, params);
  }

  me() {
    return this.http.get<UserResponse>(`${API_URL}/auth/me`);
  }

  logout() {
    return this.http.post(`${API_URL}/auth/logout`, null);
  }
}
