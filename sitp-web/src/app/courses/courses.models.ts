export interface CourseSummary {
  id: number;
  title: string;
  description: string;
  targetAudience: 'INTERN' | 'OUTSIDER';
}

export interface ChapterSummary {
  id: number;
  title: string;
  position: number;
  videoUrl: string | null;
}

export interface CourseDetail {
  id: number;
  title: string;
  description: string;
  targetAudience: 'INTERN' | 'OUTSIDER';
  price?: number | null;
  priceVisible: boolean;
  chapters: ChapterSummary[];
}
