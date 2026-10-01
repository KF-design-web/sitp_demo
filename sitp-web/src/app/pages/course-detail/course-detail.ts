
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CourseService } from '../../courses/courses.service';
import { CourseDetail } from '../../courses/courses.models';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { Button } from '../../ui/button/button';
import { LangService } from '../../core/lang.service';

@Component({
  selector: 'app-course-detail-page',
  imports: [ErrorSlip, Button],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto max-w-4xl px-8 py-8">
      @if (course() === null && !notFound() && !error()) {
        <div class="h-10 w-2/3 animate-pulse rounded-xl bg-line"></div>
        <div class="mt-4 h-6 w-1/3 animate-pulse rounded-xl bg-line"></div>
        <div class="mt-8 space-y-3">
          @for (s of skeletons; track s) {
            <div class="h-16 animate-pulse rounded-xl bg-line"></div>
          }
        </div>
      } @else if (notFound()) {
        <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 text-center shadow-card">
          <h1 class="text-lg font-semibold text-ink">Course not found</h1>
          <p class="mt-2 text-sm text-muted">This course does not exist.</p>
          <div class="mt-6">
            <app-button variant="secondary" (click)="backToCourses()">
              Back to courses
            </app-button>
          </div>
        </div>
      } @else if (error()) {
        <app-error-slip [message]="t('error.unreachable')" />
        <div class="mt-4 max-w-xs">
          <app-button variant="secondary" (click)="load()">
            Try again
          </app-button>
        </div>
      } @else if (course() !== null) {
        <h1 class="text-[28px] font-bold text-ink">{{ course()!.title }}</h1>
        <p class="mt-2 text-base text-muted">{{ course()!.description }}</p>
        <div class="mt-4 flex items-center gap-3">
          <span
            class="inline-flex items-center rounded-full border px-3 py-1 text-sm font-medium"
            [class]="course()!.targetAudience === 'INTERN'
              ? 'border-primary bg-primary-soft text-primary'
              : 'border-line bg-bg text-muted'"
          >
            {{ course()!.targetAudience }}
          </span>
          @if (course()!.priceVisible && course()!.price != null) {
            <span class="text-lg font-bold text-primary">{{ course()!.price }} FCFA</span>
          }
        </div>

        <h2 class="mt-10 text-xl font-semibold text-ink">Chapters</h2>
        <ol class="mt-4 space-y-3">
          @for (chapter of course()!.chapters; track chapter.id) {
            <li class="flex items-center gap-4 rounded-xl border border-line bg-surface p-4 shadow-card">
              <span class="w-8 text-center text-lg font-bold text-primary">{{ chapter.position }}</span>
              <span class="flex-1 font-medium text-ink">{{ chapter.title }}</span>
            </li>
          }
        </ol>
      }
    </section>
  `,
})
export class CourseDetailPage {
  private courseService = inject(CourseService);
  private langService = inject(LangService);
  private route = inject(ActivatedRoute);

  readonly course = signal<CourseDetail | null>(null);
  readonly notFound = signal(false);
  readonly error = signal(false);

  readonly skeletons = [1, 2, 3, 4];

  private readonly courseId = computed(() =>
    Number(this.route.snapshot.paramMap.get('id')),
  );

  constructor() {
    this.load();
  }

  load(): void {
    this.error.set(false);
    this.notFound.set(false);
    this.course.set(null);

    this.courseService.getById(this.courseId()).subscribe({
      next: (card) => this.course.set(card),
      error: (err: { status?: number }) => {
        if (err?.status === 404) {
          this.notFound.set(true);
        } else {
          this.error.set(true);
        }
      },
    });
  }

  backToCourses(): void {
    window.history.back();
  }

  t(key: string): string {
    return this.langService.t(key);
  }
}
