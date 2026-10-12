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

@Component({
  selector: 'app-codes-list-page',
  imports: [ErrorSlip, Button],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto max-w-3xl px-8 py-8">
      <h1 class="text-[28px] font-bold text-ink">{{ t('codes.list-heading') }}</h1>
      <p class="mt-2 text-sm text-muted">{{ t('codes.list-subtitle') }}</p>

      <div class="mt-8 rounded-xl border border-line bg-surface p-6 shadow-card">
        <label class="block text-sm font-medium text-ink" for="list-course">
          {{ t('codes.create-course-label') }}
        </label>
        <select
          id="list-course"
          class="mt-2 w-full rounded-xl border border-line bg-bg px-4 py-3 text-base text-ink focus:outline-none focus:ring-2 focus:ring-primary"
          [value]="courseId() ?? ''"
          (change)="onCourseChange($event)"
        >
          <option value="" disabled>{{ t('codes.create-course-placeholder') }}</option>
          @for (course of courses(); track course.id) {
            <option [value]="course.id">{{ tText(course.title) }}</option>
          }
        </select>

        @if (loading()) {
          <p class="mt-4 text-sm text-muted">{{ t('codes.list-loading') }}</p>
        }

        @if (listError()) {
          <div class="mt-4">
            <app-error-slip [message]="listError()" />
          </div>
          <div class="mt-4 max-w-xs">
            <app-button variant="secondary" (click)="load()">
              {{ t('courses.retry') }}
            </app-button>
          </div>
        }
      </div>

      @if (rows() !== null) {
        <div class="mt-8 rounded-xl border border-line bg-surface p-6 shadow-card">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-ink">{{ t('codes.list-table-heading') }}</h2>
            <span class="text-sm text-muted">
              {{ rows()!.length }} {{ t('codes.created-count-suffix') }}
            </span>
          </div>

          @if (rows()!.length === 0) {
            <p class="mt-4 text-sm text-muted">{{ t('codes.list-empty') }}</p>
          } @else {
            <div class="mt-4 hidden sm:block">
              <table class="w-full text-left text-sm">
                <thead>
                  <tr class="border-b border-line text-xs uppercase text-muted">
                    <th class="py-2 pr-4 font-medium">{{ t('codes.list-col-code') }}</th>
                    <th class="py-2 pr-4 font-medium">{{ t('codes.list-col-status') }}</th>
                    <th class="py-2 pr-4 font-medium">{{ t('codes.list-col-claimed') }}</th>
                    <th class="py-2 font-medium">{{ t('codes.list-col-created') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-line">
                  @for (row of rows()!; track row.id) {
                    <tr>
                      <td class="py-3 pr-4 font-mono font-semibold text-ink">{{ row.code }}</td>
                      <td class="py-3 pr-4">
                        <span
                          class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-medium"
                          [class]="statusClass(row.status)"
                        >
                          {{ t('codes.list-status-' + row.status) }}
                        </span>
                      </td>
                      <td class="py-3 pr-4 text-muted">
                        {{ row.claimantUsername ?? t('codes.list-unclaimed') }}
                      </td>
                      <td class="py-3 text-muted">{{ formatDate(row.createdAt) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <ul class="mt-2 divide-y divide-line sm:hidden">
              @for (row of rows()!; track row.id) {
                <li class="py-3">
                  <div class="flex items-center justify-between gap-4">
                    <span class="font-mono text-base font-semibold text-ink">{{ row.code }}</span>
                    <span
                      class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-medium"
                      [class]="statusClass(row.status)"
                    >
                      {{ t('codes.list-status-' + row.status) }}
                    </span>
                  </div>
                  <p class="mt-1 text-xs text-muted">
                    {{ t('codes.list-col-claimed') }}:
                    {{ row.claimantUsername ?? t('codes.list-unclaimed') }}
                  </p>
                </li>
              }
            </ul>
          }
        </div>
      }
    </section>
  `,
})
export class CodesListPage {
  private courseService = inject(CourseService);
  private codesService = inject(CodesService);
  private langService = inject(LangService);

  readonly courses = signal<CourseSummary[]>([]);
  readonly courseId = signal<number | null>(null);
  readonly rows = signal<CreatedCode[] | null>(null);
  readonly loading = signal(false);
  readonly listError = signal('');

  readonly canPick = computed(() => this.courseId() !== null);

  constructor() {
    this.courseService.getAll().subscribe({
      next: (list) => this.courses.set(list),
      error: () => this.listError.set(this.t('error.unreachable')),
    });
  }

  onCourseChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.courseId.set(value === '' ? null : Number(value));
    this.rows.set(null);
    this.listError.set('');
    if (this.courseId() !== null) {
      this.load();
    }
  }

  load(): void {
    const id = this.courseId();
    if (id === null) {
      return;
    }
    this.listError.set('');
    this.loading.set(true);
    this.rows.set(null);
    this.codesService.list(id).subscribe({
      next: (rows) => {
        this.loading.set(false);
        this.rows.set(rows);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        this.listError.set(
          err?.status === 404
            ? this.t('codes.create-course-missing')
            : err?.status === 403
              ? this.t('error.403-access')
              : this.t('error.unreachable'),
        );
      },
    });
  }

  statusClass(status: 'ACTIVE' | 'REVOKED'): string {
    return status === 'ACTIVE'
      ? 'border-primary bg-primary/10 text-primary'
      : 'border-line bg-bg text-muted line-through';
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '';
    }
    const d = new Date(iso);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
      d.getDate(),
    ).padStart(2, '0')}`;
  }

  tText(en: string): string {
    return this.langService.tText(en);
  }

  t(key: string): string {
    return this.langService.t(key);
  }
}
