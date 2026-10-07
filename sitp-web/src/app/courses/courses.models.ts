export interface CourseSummary {
  id: number;
  title: string;
  description: string;
  targetAudience: 'INTERN' | 'OUTSIDER';
}

const COURSE_THUMBNAILS: Record<string, string> = {
  'Git Foundations': 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=640&h=360&fit=crop&fm=jpg&q=70',
  'Graphic Design Fundamentals': 'https://images.unsplash.com/photo-1626785774573-4b799315345d?w=640&h=360&fit=crop&fm=jpg&q=70',
  'Port Logistics & Freight Operations': 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=640&h=360&fit=crop&fm=jpg&q=70',
  'HR Essentials for the Port Workforce': 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=640&h=360&fit=crop&fm=jpg&q=70',
  'Freight Documentation & Customs English': 'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=640&h=360&fit=crop&fm=jpg&q=70',
  'Workplace Communication (EN/FR)': 'https://images.unsplash.com/photo-1611224923853-80b023f02d71?w=640&h=360&fit=crop&fm=jpg&q=70',
};

export function courseThumbnailUrl(courseId: number, title: string): string {
  return COURSE_THUMBNAILS[title] ?? `https://picsum.photos/seed/sitp-course-${courseId}/640/360`;
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
