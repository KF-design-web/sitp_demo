import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_URL } from '../core/api-url';
import {
  AuthResponse,
  CaptchaChallenge,
  LoginPayload,
  RegisterPayload,
  UserResponse,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  register(body: RegisterPayload) {
    let params = new HttpParams()
      .set('email', body.email)
      .set('username', body.username)
      .set('password', body.password)
      .set('passwordConfirm', body.passwordConfirm)
      .set('firstName', body.firstName)
      .set('lastName', body.lastName)
      .set('captchaId', body.captchaId)
      .set('captchaAnswer', body.captchaAnswer);
    if (body.phoneNumber) {
      params = params.set('phoneNumber', body.phoneNumber);
    }
    if (body.address) {
      params = params.set('address', body.address);
    }
    if (body.country) {
      params = params.set('country', body.country);
    }
    if (body.gender) {
      params = params.set('gender', body.gender);
    }
    return this.http.post<UserResponse>(`${API_URL}/auth/register`, params);
  }

  login(body: LoginPayload) {
    const params = new HttpParams()
      .set('username', body.username)
      .set('password', body.password)
      .set('captchaId', body.captchaId)
      .set('captchaAnswer', body.captchaAnswer);
    return this.http.post<AuthResponse>(`${API_URL}/auth/login`, params);
  }

  captcha() {
    return this.http.get<CaptchaChallenge>(`${API_URL}/auth/captcha`);
  }

  verifyEmail(username: string, code: string) {
    const params = new HttpParams().set('username', username).set('code', code);
    return this.http.post(`${API_URL}/verification/verify-email`, params);
  }

  forgotPassword(email: string) {
    const params = new HttpParams().set('email', email);
    return this.http.post(`${API_URL}/verification/forgot-password`, params);
  }

  resetPassword(body: {
    username: string;
    code: string;
    newPassword: string;
    newPasswordConfirm: string;
  }) {
    const params = new HttpParams()
      .set('username', body.username)
      .set('code', body.code)
      .set('newPassword', body.newPassword)
      .set('newPasswordConfirm', body.newPasswordConfirm);
    return this.http.post(`${API_URL}/verification/reset-password`, params);
  }

  me() {
    return this.http.get<UserResponse>(`${API_URL}/auth/me`);
  }

  logout() {
    return this.http.post(`${API_URL}/auth/logout`, null);
  }
}
