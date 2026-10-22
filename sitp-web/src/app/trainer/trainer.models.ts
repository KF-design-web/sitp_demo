export interface TrainerCourse {
  id: number;
  title: string;
  description: string;
  labels: string | null;
  thumbnailUrl: string | null;
  targetAudience: 'INTERN' | 'OUTSIDER';
  price?: number | null;
  priceVisible: boolean;
}

export interface TrainerChapter {
  id: number;
  courseId: number;
  title: string;
  description: string;
  labels: string | null;
  videoUrl: string | null;
  thumbnailUrl: string | null;
  position: number;
}

export interface TrainerCourseDetail {
  id: number;
  title: string;
  description: string;
  labels: string | null;
  thumbnailUrl: string | null;
  targetAudience: 'INTERN' | 'OUTSIDER';
  price?: number | null;
  priceVisible: boolean;
  chapters: TrainerChapter[];
}

export interface CourseFormValues {
  title: string;
  description: string;
  labels: string;
  thumbnailUrl: string;
  targetAudience?: 'INTERN' | 'OUTSIDER';
  price?: string;
}

export interface EpisodeFormValues {
  title: string;
  description: string;
  labels: string;
  videoUrl: string;
  thumbnailUrl: string;
  position?: string;
}
