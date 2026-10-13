
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseService } from '../../courses/courses.service';
import { CourseDetail } from '../../courses/courses.models';
import { formatXaf } from '../../courses/format-xaf';
import { CodesService } from '../../codes/codes.service';
import { RedeemConfirmation } from '../../codes/codes.models';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { Button } from '../../ui/button/button';
import { LangService } from '../../core/lang.service';

@Component({
  selector: 'app-course-detail-page',
  imports: [ErrorSlip, Button],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto max-w-4xl px-8 py-8">
      @if (course() === null && !notFound() && !error() && !locked() && confirmation() === null) {
        <div class="h-10 w-2/3 animate-pulse rounded-xl bg-line"></div>
        <div class="mt-4 h-6 w-1/3 animate-pulse rounded-xl bg-line"></div>
        <p class="mt-6 text-sm text-muted">{{ t('detail.loading') }}</p>
        <div class="mt-8 space-y-3">
          @for (s of skeletons; track s) {
            <div class="flex h-20 items-center gap-4">
              <div class="h-14 w-24 animate-pulse rounded-xl bg-line"></div>
              <div class="h-6 flex-1 animate-pulse rounded-xl bg-line"></div>
            </div>
          }
        </div>
      } @else if (notFound()) {
        <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 text-center shadow-card">
          <h1 class="text-lg font-semibold text-ink">{{ t('detail.not-found-title') }}</h1>
          <p class="mt-2 text-sm text-muted">{{ t('detail.not-found-text') }}</p>
          <div class="mt-6">
            <app-button variant="secondary" (click)="backToCourses()">
              {{ t('detail.back-to-courses') }}
            </app-button>
          </div>
        </div>
      } @else if (error()) {
        <h2 class="text-lg font-semibold text-ink">{{ t('detail.error-title') }}</h2>
        <div class="mt-6">
          <app-error-slip [message]="t('error.unreachable')" />
        </div>
        <div class="mt-4 max-w-xs">
          <app-button variant="secondary" (click)="load()">
            {{ t('detail.retry') }}
          </app-button>
        </div>
      } @else if (confirmation() !== null) {
        <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 text-center shadow-card">
          <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary-soft">
            <svg viewBox="0 0 24 24" class="h-6 w-6 text-primary" fill="currentColor" aria-hidden="true">
              <path d="M9 16.2l-3.5-3.5 1.4-1.4L9 13.4l8.1-8.1 1.4 1.4z" />
            </svg>
          </div>
          <h1 class="mt-4 text-lg font-semibold text-ink">{{ t('codes.redeemed-title') }}</h1>
          <p class="mt-2 text-sm text-muted">
            {{ t('codes.redeemed-text') }}
            <span class="font-semibold text-ink">{{ tText(confirmation()!.courseTitle) }}</span>
          </p>
          <p class="mt-1 text-sm text-muted">{{ accessUntilText(confirmation()!.accessUntil) }}</p>
          <div class="mt-6">
            <app-button (click)="openTheCourse()">
              {{ t('codes.redeemed-open') }}
            </app-button>
          </div>
        </div>
      } @else if (locked()) {
        <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 shadow-card">
          <h1 class="text-lg font-semibold text-ink">{{ t('codes.locked-title') }}</h1>
          <p class="mt-2 text-sm text-muted">{{ t('codes.locked-text') }}</p>

          <label class="mt-6 block text-sm font-medium text-ink" for="redeem-code">
            {{ t('codes.redeem-label') }}
          </label>
          <input
            id="redeem-code"
            type="text"
            [placeholder]="t('codes.redeem-placeholder')"
            class="mt-2 w-full rounded-xl border border-line bg-bg px-4 py-3 font-mono text-base uppercase text-ink focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="redeemCode()"
            (input)="onRedeemCodeInput($event)"
          />
          @if (redeemError()) {
            <div class="mt-3">
              <app-error-slip [message]="redeemError()" />
            </div>
          }
          <div class="mt-4">
            <app-button
              type="button"
              [loading]="redeeming()"
              [loadingLabel]="t('codes.redeem-loading')"
              (click)="redeem()"
            >
              {{ t('codes.redeem-submit') }}
            </app-button>
          </div>

          <div class="mt-6">
            <app-button variant="secondary" (click)="backToCourses()">
              {{ t('detail.back-to-courses') }}
            </app-button>
          </div>
        </div>
      } @else if (course() !== null) {
        <button
          type="button"
          (click)="backToCourses()"
          class="text-sm font-medium text-muted hover:text-ink hover:underline focus:outline-none"
        >
          ← {{ t('detail.back-to-courses') }}
        </button>

        <h1 class="mt-4 text-[28px] font-bold text-ink">{{ tText(course()!.title) }}</h1>
        <p class="mt-2 whitespace-pre-line text-base text-muted">{{ tText(course()!.description) }}</p>
        <div class="mt-4 flex flex-wrap items-center gap-3">
          <span
            class="inline-flex items-center rounded-full border px-3 py-1 text-sm font-medium"
            [class]="course()!.targetAudience === 'INTERN'
              ? 'border-primary bg-primary-soft text-primary'
              : 'border-line bg-bg text-muted'"
          >
            {{ t('courses.audience-' + course()!.targetAudience) }}
          </span>
          @if (course()!.priceVisible && course()!.price != null) {
            <span class="text-lg font-bold text-primary">{{ formatXaf(course()!.price!) }}</span>
          }
        </div>

        <h2 class="mt-10 text-xl font-semibold text-ink">{{ t('detail.chapters-heading') }}</h2>
        <ol class="mt-4 space-y-3">
          @for (chapter of course()!.chapters; track chapter.id) {
            <li class="flex items-center gap-4 rounded-xl border border-line bg-surface p-4 shadow-card">
              <span class="w-8 text-center text-lg font-bold text-primary">{{ chapter.position }}</span>
              <div class="relative h-14 w-24 shrink-0 overflow-hidden rounded-lg bg-gradient-to-br from-primary-soft to-line">
                <img
                  [src]="thumbnailUrl(chapter.id)"
                  [alt]="tText(chapter.title)"
                  class="h-full w-full object-cover"
                  (error)="failThumbnail(chapter.id)"
                  [hidden]="thumbFailed().has(chapter.id)"
                />
                @if (thumbFailed().has(chapter.id)) {
                  <div class="absolute inset-0 flex items-center justify-center">
                    <svg viewBox="0 0 24 24" class="h-6 w-6 text-primary" fill="currentColor" aria-hidden="true">
                      <path d="M8 5v14l11-7z" />
                    </svg>
                  </div>
                }
              </div>
              <span class="flex-1 font-medium text-ink">{{ tText(chapter.title) || t('detail.chapter-fallback-title') }}</span>
            </li>
          }
        </ol>
        <p class="mt-4 text-sm text-muted">{{ t('detail.video-coming-soon') }}</p>
      }
    </section>
  `,
})
export class CourseDetailPage {
  private courseService = inject(CourseService);
  private codesService = inject(CodesService);
  private langService = inject(LangService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly course = signal<CourseDetail | null>(null);
  readonly notFound = signal(false);
  readonly error = signal(false);
  readonly locked = signal(false);
  readonly confirmation = signal<RedeemConfirmation | null>(null);
  readonly redeemCode = signal('');
  readonly redeeming = signal(false);
  readonly redeemError = signal('');

  readonly skeletons = [1, 2, 3, 4];

  thumbnailUrl(chapterId: number): string {
    return `https://picsum.photos/seed/sitp-chapter-${chapterId}/240/135`;
  }

  readonly thumbFailed = signal<Set<number>>(new Set());

  failThumbnail(chapterId: number): void {
    this.thumbFailed.update((current) => new Set(current).add(chapterId));
  }

  private readonly courseId = computed(() =>
    Number(this.route.snapshot.paramMap.get('id')),
  );

  constructor() {
    this.load();
  }

  load(): void {
    this.error.set(false);
    this.notFound.set(false);
    this.locked.set(false);
    this.course.set(null);

    this.courseService.getById(this.courseId()).subscribe({
      next: (card) => this.course.set(card),
      error: (err: { status?: number }) => {
        if (err?.status === 404) {
          this.notFound.set(true);
        } else if (err?.status === 401) {
          this.router.navigate(['/login'], { queryParams: { from: 'detail' } });
        } else if (err?.status === 403) {
          this.locked.set(true);
        } else {
          this.error.set(true);
        }
      },
    });
  }

  onRedeemCodeInput(event: Event): void {
    this.redeemCode.set((event.target as HTMLInputElement).value);
  }

  redeem(): void {
    const code = this.redeemCode().trim();
    if (!code) {
      this.redeemError.set(this.t('codes.redeem-required'));
      return;
    }

    this.redeemError.set('');
    this.redeeming.set(true);
    this.codesService.redeem(code).subscribe({
      next: (card) => {
        this.redeeming.set(false);
        this.locked.set(false);
        this.error.set(false);
        this.notFound.set(false);
        this.confirmation.set(card);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.redeeming.set(false);
        const apiMsg = err?.error?.message;
        this.redeemError.set(
          err?.status === 409
            ? (apiMsg ? this.tApiError(apiMsg) : this.t('error.400-code'))
            : err?.status === 401
              ? this.t('detail.login-notice')
              : this.t('error.unreachable'),
        );
      },
    });
  }

  openTheCourse(): void {
    this.confirmation.set(null);
    this.redeemCode.set('');
    this.redeemError.set('');
    this.load();
  }

  backToCourses(): void {
    this.router.navigate(['/courses']);
  }

  readonly formatXaf = formatXaf;

  t(key: string): string {
    return this.langService.t(key);
  }

  tApiError(msg?: string): string {
    return this.langService.tApiError(msg);
  }

  accessUntilText(accessUntil: string): string {
    if (accessUntil === 'lifetime') {
      return this.t('codes.access-lifetime');
    }
    const match = accessUntil.match(/^(\d+) days left$/);
    if (match) {
      return this.t('codes.access-days').replace('{{days}}', match[1]);
    }
    return accessUntil;
  }

  tText(en: string): string {
    return this.langService.tText(en);
  }
}
