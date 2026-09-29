
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CourseService } from '../../courses/courses.service';
import { CourseSummary } from '../../courses/courses.models';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { Button } from '../../ui/button/button';
import { LangService } from '../../core/lang.service';
import { CourseCard } from '../../ui/course-card/course-card';

@Component({
  selector: 'app-catalog-page',
  imports: [ErrorSlip, Button, CourseCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto max-w-6xl px-8 py-8">
      @if (courses() === null && !error()) {
        <div class="grid grid-cols-1 items-start gap-6 md:grid-cols-2 lg:grid-cols-3">
          @for (s of skeletons; track s) {
            <div class="h-64 animate-pulse rounded-xl bg-line"></div>
          }
        </div>
      } @else if (error()) {
        <h1 class="text-[28px] font-bold text-ink">{{ t('courses.heading') }}</h1>
        <div class="mt-6">
          <app-error-slip [message]="t('error.unreachable')" />
        </div>
        <div class="mt-4 max-w-xs">
          <app-button variant="secondary" (click)="retry()">
            {{ t('courses.retry') }}
          </app-button>
        </div>
      } @else if (isEmpty()) {
        <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 text-center shadow-card">
          <h2 class="text-lg font-semibold text-ink">{{ t('courses.empty-title') }}</h2>
          <p class="mt-2 text-sm text-muted">{{ t('courses.empty-text') }}</p>
        </div>
      } @else if (hasCourses()) {
        <h1 class="text-[28px] font-bold text-ink">{{ t('courses.heading') }}</h1>

        <div class="mt-6 flex flex-wrap items-center gap-2">
          @for (option of filterOptions; track option.value) {
            <button
              type="button"
              (click)="filter.set(option.value)"
              class="inline-flex items-center rounded-full border px-3 py-1 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary"
              [class]="filter() === option.value
                ? 'border-primary bg-primary text-white'
                : 'border-line bg-surface text-muted hover:bg-bg'"
            >
              {{ t(option.label) }}
            </button>
          }
        </div>

        <div class="mt-6 grid grid-cols-1 items-start gap-6 md:grid-cols-2 lg:grid-cols-3">
          @for (course of visible(); track course.id) {
            <app-course-card [course]="course" />
          }
        </div>
      }
    </section>
  `,
})
export class CatalogPage {
  private courseService = inject(CourseService);
  private langService = inject(LangService);

  readonly courses = signal<CourseSummary[] | null>(null);
  readonly error = signal(false);

  readonly skeletons = [1, 2, 3, 4, 5, 6];

  readonly isEmpty = computed(() => this.courses()?.length === 0);
  readonly hasCourses = computed(() => (this.courses()?.length ?? 0) > 0);

  readonly filter = signal<'ALL' | 'INTERN' | 'OUTSIDER'>('ALL');

  readonly filterOptions = [
    { value: 'ALL', label: 'courses.filter-all' },
    { value: 'INTERN', label: 'courses.filter-intern' },
    { value: 'OUTSIDER', label: 'courses.filter-outsider' },
  ] as const;

  readonly visible = computed(() => {
    const list = this.courses();
    if (list === null || this.filter() === 'ALL') {
      return list;
    }
    return list.filter((c) => c.targetAudience === this.filter());
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.error.set(false);
    this.courseService.getAll().subscribe({
      next: (list) => this.courses.set(list),
      error: () => this.error.set(true),
    });
  }

  retry(): void {
    this.load();
  }

  t(key: string): string {
    return this.langService.t(key);
  }
}
