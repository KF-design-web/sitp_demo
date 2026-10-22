import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { ProfileWidget } from '../../ui/profile-widget/profile-widget';
import { MyCourseResponse } from '../../auth/auth.models';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, Button, ErrorSlip, ProfileWidget],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-6xl px-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-[28px] font-bold text-ink">{{ t('dashboard.heading') }}</h1>
        <div class="flex items-center gap-3">
          <app-button variant="primary" compact routerLink="/courses">
            + {{ t('dashboard.addCourse') }}
          </app-button>
          <app-profile-widget />
        </div>
      </div>

      <div class="mt-6">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (loading()) {
        <p class="mt-10 text-sm text-muted">{{ t('dashboard.loading') }}</p>
      } @else if (courses().length === 0) {
        <div class="mx-auto mt-10 max-w-xl rounded-xl border border-dashed border-line bg-surface p-10 text-center">
          <p class="text-base font-medium text-ink">{{ t('dashboard.emptyTitle') }}</p>
          <p class="mt-1 text-sm text-muted">{{ t('dashboard.emptyText') }}</p>
          <app-button variant="primary" compact routerLink="/courses" class="mt-4">
            {{ t('dashboard.browseCourses') }}
          </app-button>
        </div>
      } @else {
        <div class="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          @for (course of courses(); track course.courseId) {
            <div class="rounded-xl border border-line bg-surface p-4 shadow-card">
              <div class="flex items-start justify-between gap-2">
                <h3 class="text-sm font-semibold text-ink leading-snug">{{ course.courseTitle }}</h3>
                <span
                  class="shrink-0 rounded-full px-2 py-0.5 text-xs font-medium"
                  [class]="statusClass(course)"
                >{{ statusLabel(course) }}</span>
              </div>
              <p class="mt-1 text-xs text-muted">{{ audienceLabel(course.targetAudience) }}</p>
              <p class="mt-2 text-xs font-medium text-ink">{{ accessLabel(course) }}</p>
              <app-button
                variant="secondary"
                compact
                class="mt-3"
                [disabled]="!course.accessValid"
                [routerLink]="['/courses', course.courseId]"
              >
                {{ course.accessValid ? t('dashboard.resume') : t('dashboard.statusExpired') }}
              </app-button>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class DashboardPage {
  private authService = inject(AuthService);
  private session = inject(SessionService);
  private langService = inject(LangService);
  private router = inject(Router);

  readonly user = this.session.user;
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly courses = signal<MyCourseResponse[]>([]);

  constructor() {
    if (this.session.user() === null) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadCourses();
  }

  t(key: string): string {
    return this.langService.t(key);
  }

  statusClass(course: MyCourseResponse): string {
    if (!course.accessValid) return 'bg-red-100 text-red-700';
    return 'bg-green-100 text-green-700';
  }

  statusLabel(course: MyCourseResponse): string {
    if (!course.accessValid) return this.t('dashboard.statusExpired');
    return this.t('dashboard.statusActive');
  }

  audienceLabel(audience: string): string {
    return audience === 'INTERN' ? this.t('courses.audience-INTERN') : this.t('courses.audience-OUTSIDER');
  }

  accessLabel(course: MyCourseResponse): string {
    if (course.accessUntil === 'lifetime') return this.t('codes.access-lifetime');
    return course.accessUntil ?? '';
  }

  loadCourses(): void {
    this.authService.myCourses().subscribe({
      next: (courses) => {
        this.courses.set(courses);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.t('error.fallback'));
        this.loading.set(false);
      },
    });
  }
}
