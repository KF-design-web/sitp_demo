import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { CourseService } from '../../courses/courses.service';
import { CourseSummary } from '../../courses/courses.models';
import { CodesService } from '../../codes/codes.service';
import { CreatedCode } from '../../codes/codes.models';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { Button } from '../../ui/button/button';
import { ConfirmModal } from '../../ui/confirm-modal/confirm-modal';
import { LangService } from '../../core/lang.service';

@Component({
  selector: 'app-codes-list-page',
  imports: [ErrorSlip, Button, ConfirmModal, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto max-w-5xl px-8 py-8">
      <h1 class="text-[28px] font-bold text-ink">{{ t('codes.list-heading') }}</h1>
      <p class="mt-2 text-sm text-muted">{{ t('codes.list-subtitle') }}</p>

      <div class="mt-8 rounded-xl border border-line bg-surface p-6 shadow-card">
        <h2 class="text-lg font-semibold text-ink">{{ t('codes.list-course-heading') }}</h2>
        <p class="mt-1 text-xs text-muted">{{ t('codes.list-course-hint') }}</p>

        @if (coursesError()) {
          <div class="mt-4">
            <app-error-slip [message]="coursesError()" />
          </div>
          <div class="mt-4 max-w-xs">
            <app-button variant="secondary" (click)="loadCourses()">
              {{ t('courses.retry') }}
            </app-button>
          </div>
        }

        @if (coursesLoading()) {
          <p class="mt-4 text-sm text-muted">{{ t('codes.list-loading') }}</p>
        }

        <ul class="mt-4 divide-y divide-line">
          @for (course of courses(); track course.id) {
            <li class="flex flex-wrap items-center justify-between gap-3 py-3">
              <div>
                <p class="text-sm font-medium text-ink">{{ tText(course.title) }}</p>
                <p class="text-xs text-muted">
                  {{ course.targetAudience === 'INTERN' ? t('courses.audience-INTERN') : t('courses.audience-OUTSIDER') }}
                </p>
              </div>
              <div class="flex items-center gap-2">
                <app-button
                  variant="secondary"
                  compact
                  [disabled]="loading() || revoking()"
                  (click)="selectCourse(course)"
                >
                  {{ t('codes.list-show') }}
                </app-button>
                <app-button variant="primary" compact [routerLink]="'/codes/create'" [queryParams]="{ courseId: course.id }">
                  {{ t('codes.list-generate') }}
                </app-button>
              </div>
            </li>
          } @empty {
            @if (!coursesLoading()) {
              <li class="py-3 text-sm text-muted">{{ t('codes.list-course-empty') }}</li>
            }
          }
        </ul>
      </div>

      @if (selectedTitle() !== null) {
        <div class="mt-6 rounded-xl border border-line bg-surface p-6 shadow-card">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-ink">
              {{ t('codes.list-table-heading') }} — {{ selectedTitle() }}
            </h2>
            <span class="text-sm text-muted">
              {{ rows()?.length ?? 0 }} {{ t('codes.created-count-suffix') }}
            </span>
          </div>

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

          @if (rows() !== null && !loading() && !listError()) {
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
                      <th class="py-2 pr-4 font-medium">{{ t('codes.list-col-created') }}</th>
                      <th class="py-2 font-medium"></th>
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
                        <td class="py-3 text-right">
                          @if (row.status === 'ACTIVE') {
                            <app-button
                              variant="secondary"
                              compact
                              [disabled]="revokeAsk() !== null || revoking()"
                              (click)="askRevoke(row.code)"
                            >
                              {{ t('codes.revoke') }}
                            </app-button>
                          }
                        </td>
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
                    @if (row.status === 'ACTIVE') {
                      <div class="mt-2 max-w-[10rem]">
                        <app-button
                          variant="secondary"
                          compact
                          [disabled]="revokeAsk() !== null || revoking()"
                          (click)="askRevoke(row.code)"
                        >
                          {{ t('codes.revoke') }}
                        </app-button>
                      </div>
                    }
                  </li>
                }
              </ul>
            }
          }
          @if (revokeError()) {
            <div class="mt-4">
              <app-error-slip [message]="revokeError()" />
            </div>
          }
        </div>
      }

      @if (revokeAsk() !== null) {
        <app-confirm-modal
          [title]="t('codes.revoke-confirm-title')"
          [text]="t('codes.revoke-confirm-text')"
          [confirmLabel]="t('codes.revoke-confirm-go')"
          [cancelLabel]="t('codes.revoke-cancel')"
          (confirm)="doRevoke()"
          (cancel)="closeRevoke()"
        />
      }
    </section>
  `,
})
export class CodesListPage {
  private courseService = inject(CourseService);
  private codesService = inject(CodesService);
  private langService = inject(LangService);

  readonly courses = signal<CourseSummary[]>([]);
  readonly coursesLoading = signal(true);
  readonly coursesError = signal('');

  readonly courseId = signal<number | null>(null);
  readonly rows = signal<CreatedCode[] | null>(null);
  readonly loading = signal(false);
  readonly listError = signal('');
  readonly revokeAsk = signal<string | null>(null);
  readonly revoking = signal(false);
  readonly revokeError = signal('');

  constructor() {
    this.loadCourses();
  }

  loadCourses(): void {
    this.coursesError.set('');
    this.coursesLoading.set(true);
    this.courseService.getAll().subscribe({
      next: (list) => {
        this.courses.set(list);
        if (!list.some((c) => c.id === this.courseId())) {
          this.courseId.set(null);
          this.rows.set(null);
        }
        this.coursesLoading.set(false);
      },
      error: () => {
        this.coursesLoading.set(false);
        this.coursesError.set(this.t('error.unreachable'));
      },
    });
  }

  selectCourse(course: CourseSummary): void {
    if (course.id === this.courseId()) {
      // Same course clicked again — collapse.
      this.courseId.set(null);
      this.rows.set(null);
      return;
    }
    this.courseId.set(course.id);
    this.load();
  }

  selectedTitle(): string | null {
    const id = this.courseId();
    if (id === null) return null;
    const course = this.courses().find((c) => c.id === id);
    return course ? this.tText(course.title) : null;
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

  askRevoke(code: string): void {
    this.revokeError.set('');
    this.revokeAsk.set(code);
  }

  closeRevoke(): void {
    this.revokeAsk.set(null);
  }

  doRevoke(): void {
    const code = this.revokeAsk();
    if (code === null) {
      return;
    }
    this.revoking.set(true);
    this.revokeError.set('');
    this.codesService.revoke(code).subscribe({
      next: () => {
        this.revoking.set(false);
        this.revokeAsk.set(null);
        this.load();
      },
      error: (err: { error?: { message?: string } }) => {
        this.revoking.set(false);
        this.revokeAsk.set(null);
        this.revokeError.set(this.langService.tApiError(err?.error?.message));
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
