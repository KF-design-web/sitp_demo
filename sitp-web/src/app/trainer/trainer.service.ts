import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_URL } from '../core/api-url';
import { TrainerChapter, TrainerCourse, TrainerCourseDetail } from './trainer.models';

interface CourseFormBody {
  [key: string]: string | undefined;
  title: string;
  description: string;
  labels: string;
  thumbnailUrl: string;
  targetAudience?: string;
  price?: string;
}

interface EpisodeFormBody {
  [key: string]: string | undefined;
  title: string;
  description: string;
  labels: string;
  videoUrl: string;
  thumbnailUrl: string;
  position?: string;
}

@Injectable({ providedIn: 'root' })
export class TrainerService {
  private http = inject(HttpClient);

  myCourses() {
    return this.http.get<TrainerCourse[]>(`${API_URL}/courses/trainer/mine`);
  }

  courseDetail(courseId: number) {
    return this.http.get<TrainerCourseDetail>(`${API_URL}/courses/trainer/${courseId}`);
  }

  createCourse(body: CourseFormBody) {
    return this.http.post<TrainerCourse>(`${API_URL}/courses/trainer`, this.toParams(body));
  }

  updateCourse(courseId: number, body: CourseFormBody) {
    return this.http.put<TrainerCourse>(`${API_URL}/courses/${courseId}`, this.toParams(body));
  }

  deleteCourse(courseId: number) {
    return this.http.delete<void>(`${API_URL}/courses/${courseId}`);
  }

  addEpisode(courseId: number, body: EpisodeFormBody) {
    return this.http.post<TrainerChapter>(`${API_URL}/courses/${courseId}/chapters`, this.toParams(body));
  }

  updateEpisode(episodeId: number, body: EpisodeFormBody) {
    return this.http.put<TrainerChapter>(`${API_URL}/chapters/${episodeId}`, this.toParams(body));
  }

  deleteEpisode(episodeId: number) {
    return this.http.delete<void>(`${API_URL}/chapters/${episodeId}`);
  }

  private toParams(body: Record<string, string | undefined>): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(body)) {
      if (value !== undefined && value !== '') {
        params = params.set(key, value);
      }
    }
    return params;
  }
}
