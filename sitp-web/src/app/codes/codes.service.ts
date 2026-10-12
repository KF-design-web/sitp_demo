import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_URL } from '../core/api-url';
import { CreatedCode, RedeemConfirmation } from './codes.models';

@Injectable({ providedIn: 'root' })
export class CodesService {
  private http = inject(HttpClient);

  list(courseId: number) {
    const params = new HttpParams().set('courseId', String(courseId));
    return this.http.get<CreatedCode[]>(`${API_URL}/codes`, { params });
  }

  create(courseId: number, count: number) {
    const params = new HttpParams()
      .set('courseId', String(courseId))
      .set('count', String(count));
    return this.http.post<CreatedCode[]>(`${API_URL}/codes`, params);
  }

  redeem(code: string) {
    return this.http.post<RedeemConfirmation>(
      `${API_URL}/codes/${encodeURIComponent(code)}/redeem`,
      null,
    );
  }
}
