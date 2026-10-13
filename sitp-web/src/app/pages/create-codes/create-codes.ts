import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CourseService } from '../../courses/courses.service';
import { CourseSummary } from '../../courses/courses.models';
import { CodesService } from '../../codes/codes.service';
import { CreatedCode } from '../../codes/codes.models';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { Button } from '../../ui/button/button';
import { LangService } from '../../core/lang.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-create-codes-page',
  imports: [ErrorSlip, Button, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto max-w-3xl px-8 py-8">
      <h1 class="text-[28px] font-bold text-ink">{{ t('codes.create-heading') }}</h1>
      <p class="mt-2 text-sm text-muted">{{ t('codes.create-subtitle') }}</p>

      <p class="mt-6 text-sm">
        <a
          routerLink="/codes"
          class="font-medium text-primary underline-offset-4 hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
        >{{ t('codes.created-see-all') }}</a>
      </p>

      <div class="mt-8 rounded-xl border border-line bg-surface p-6 shadow-card">
        <label class="block text-sm font-medium text-ink" for="create-course">
          {{ t('codes.create-course-label') }}
        </label>
        <select
          id="create-course"
          class="mt-2 w-full rounded-xl border border-line bg-bg px-4 py-3 text-base text-ink focus:outline-none focus:ring-2 focus:ring-primary"
          [value]="courseId() ?? ''"
          (change)="onCourseChange($event)"
        >
          <option value="" disabled>{{ t('codes.create-course-placeholder') }}</option>
          @for (course of courses(); track course.id) {
            <option [value]="course.id">{{ tText(course.title) }}</option>
          }
        </select>

        <label class="mt-6 block text-sm font-medium text-ink" for="create-count">
          {{ t('codes.create-count-label') }}
        </label>
        <input
          id="create-count"
          type="number"
          min="1"
          max="500"
          class="mt-2 w-40 rounded-xl border border-line bg-bg px-4 py-3 text-base text-ink focus:outline-none focus:ring-2 focus:ring-primary"
          [value]="count()"
          (input)="onCountChange($event)"
        />
        <p class="mt-1 text-xs text-muted">{{ t('codes.create-count-hint') }}</p>

        @if (internCourse()) {
          <label class="mt-6 block text-sm font-medium text-ink" for="create-validity">
            {{ t('codes.create-validity-label') }}
          </label>
          <input
            id="create-validity"
            type="number"
            min="1"
            max="3650"
            class="mt-2 w-40 rounded-xl border border-line bg-bg px-4 py-3 text-base text-ink focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="validityDays() ?? ''"
            (input)="onValidityChange($event)"
          />
          <p class="mt-1 text-xs text-muted">{{ t('codes.create-validity-hint') }}</p>
        }

        @if (formError()) {
          <div class="mt-4">
            <app-error-slip [message]="formError()" />
          </div>
        }

        <div class="mt-6 max-w-xs">
          <app-button
            type="button"
            [loading]="creating()"
            [loadingLabel]="t('codes.create-loading')"
            (click)="create()"
          >
            {{ t('codes.create-submit') }}
          </app-button>
        </div>
      </div>

      @if (createdCodes() !== null) {
        <div class="mt-8 rounded-xl border border-line bg-surface p-6 shadow-card">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-ink">{{ t('codes.created-heading') }}</h2>
            <span class="text-sm text-muted">
              {{ createdCodes()!.length }} {{ t('codes.created-count-suffix') }}
            </span>
          </div>
          <ul class="mt-4 divide-y divide-line">
            @for (row of createdCodes()!; track row.id) {
              <li class="flex items-center justify-between gap-4 py-3">
                <span class="font-mono text-base font-semibold text-ink">{{ row.code }}</span>
                <button
                  type="button"
                  (click)="copy(row.code)"
                  class="rounded-full border border-line px-3 py-1 text-xs font-medium text-muted transition-colors hover:bg-bg hover:text-ink focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  {{ copiedCode() === row.code ? t('codes.copied') : t('codes.copy') }}
                </button>
              </li>
            }
          </ul>
        </div>
      }
    </section>
  `,
})
export class CreateCodesPage {
  private courseService = inject(CourseService);
  private codesService = inject(CodesService);
  private langService = inject(LangService);

  readonly courses = signal<CourseSummary[]>([]);
  readonly courseId = signal<number | null>(null);
  readonly count = signal(10);
  readonly validityDays = signal<number | null>(null);
  readonly creating = signal(false);
  readonly formError = signal('');
  readonly createdCodes = signal<CreatedCode[] | null>(null);
  readonly copiedCode = signal<string | null>(null);

  readonly internCourse = computed(
    () =>
      this.courses().find((c) => c.id === this.courseId())?.targetAudience === 'INTERN',
  );

  readonly canCreate = computed(
    () =>
      this.courseId() !== null &&
      this.count() >= 1 &&
      this.count() <= 500 &&
      (!this.internCourse() || (this.validityDays() !== null && this.validityDays()! >= 1)),
  );

  constructor() {
    this.courseService.getAll().subscribe({
      next: (list) => this.courses.set(list),
      error: () => this.formError.set(this.t('error.unreachable')),
    });
  }

  onCourseChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.courseId.set(value === '' ? null : Number(value));
  }

  onValidityChange(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    this.validityDays.set(raw === '' ? null : Number(raw));
  }

  onCountChange(event: Event): void {
    this.count.set(Number((event.target as HTMLInputElement).value));
  }

  create(): void {
    if (this.courseId() === null) {
      this.formError.set(this.t('codes.create-course-required'));
      return;
    }
    const count = this.count();
    if (count < 1 || count > 500) {
      this.formError.set(this.t('codes.create-count-bounds'));
      return;
    }

    if (this.internCourse() && (this.validityDays() === null || this.validityDays()! < 1)) {
      this.formError.set(this.t('codes.create-validity-required'));
      return;
    }

    this.formError.set('');
    this.creating.set(true);
    this.createdCodes.set(null);
    const validity = this.internCourse() ? this.validityDays()! : undefined;
    this.codesService.create(this.courseId()!, count, validity).subscribe({
      next: (rows) => {
        this.creating.set(false);
        this.createdCodes.set(rows);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.creating.set(false);
        this.formError.set(
          err?.status === 404
            ? this.t('codes.create-course-missing')
            : err?.status === 400
              ? err?.error?.message
                ? this.langService.tApiError(err.error.message)
                : this.t('codes.create-count-bounds')
              : err?.status === 403
                ? this.t('error.403-access')
                : this.t('error.unreachable'),
        );
      },
    });
  }

  copy(code: string): void {
    navigator.clipboard?.writeText(code).then(() => {
      this.copiedCode.set(code);
      setTimeout(() => this.copiedCode.set(null), 1500);
    });
  }

  tText(en: string): string {
    return this.langService.tText(en);
  }

  t(key: string): string {
    return this.langService.t(key);
  }
}
