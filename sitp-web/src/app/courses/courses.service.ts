
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../core/api-url';
import { CourseSummary } from './courses.models';

@Injectable({ providedIn: 'root' })
export class CourseService {
  private http = inject(HttpClient);

  getAll() {
    return this.http.get<CourseSummary[]>(`${API_URL}/courses`);
  }
}
