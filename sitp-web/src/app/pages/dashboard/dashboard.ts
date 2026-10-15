import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { TextField } from '../../ui/text-field/text-field';
import { MyCourseResponse, UserResponse } from '../../auth/auth.models';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, Button, ErrorSlip, TextField, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-6xl px-4">
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[300px_1fr]">

        <aside class="lg:sticky lg:top-8 lg:self-start rounded-xl border border-line bg-surface p-6 shadow-card">
          <div class="flex flex-col items-center text-center">
            @if (user()?.profilePictureUrl) {
              <img
                [src]="user()!.profilePictureUrl"
                alt=""
                class="h-20 w-20 rounded-full object-cover ring-2 ring-primary/20"
                (error)="onAvatarError()"
              />
            } @else {
              <div class="h-20 w-20 rounded-full bg-primary/10 flex items-center justify-center">
                <span class="text-2xl font-bold text-primary">{{ initials() }}</span>
              </div>
            }
            <h2 class="mt-3 text-lg font-semibold text-ink">{{ fullName() }}</h2>
            <p class="text-sm text-muted">@{{ user()?.username }}</p>
            <span class="mt-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
              {{ memberTypeLabel() }}
            </span>
          </div>

          <dl class="mt-6 space-y-3 text-sm">
            <div class="flex justify-between">
              <dt class="text-muted">{{ t('dashboard.memberSince') }}</dt>
              <dd class="font-medium text-ink">{{ memberSince() }}</dd>
            </div>
            @if (user()?.country) {
              <div class="flex justify-between">
                <dt class="text-muted">{{ t('dashboard.country') }}</dt>
                <dd class="font-medium text-ink">{{ user()?.country }}</dd>
              </div>
            }
            @if (user()?.birthdate) {
              <div class="flex justify-between">
                <dt class="text-muted">{{ t('dashboard.age') }}</dt>
                <dd class="font-medium text-ink">{{ age() }}</dd>
              </div>
            }
            @if (user()?.email) {
              <div class="flex justify-between">
                <dt class="text-muted">{{ t('dashboard.email') }}</dt>
                <dd class="font-medium text-ink truncate ml-2">{{ user()?.email }}</dd>
              </div>
            }
          </dl>

          <div class="mt-6 border-t border-line pt-4">
            <button
              type="button"
              class="w-full text-left text-sm font-medium text-primary hover:underline"
              (click)="startEdit()"
            >
              {{ t('dashboard.editProfile') }}
            </button>
          </div>

          @if (editing()) {
            <form
              [formGroup]="editForm"
              (ngSubmit)="saveProfile()"
              class="mt-4 space-y-3 border-t border-line pt-4"
            >
              <app-text-field
                formControlName="username"
                type="text"
                [label]="t('dashboard.editUsername')"
                [hint]="t('dashboard.editUsernameHint')"
                [error]="usernameError()"
              />
              <app-text-field
                formControlName="profilePictureUrl"
                type="text"
                [label]="t('dashboard.editPictureUrl')"
                [hint]="t('dashboard.editPictureUrlHint')"
              />
              <div class="flex gap-2">
                <app-button type="submit" compact [loading]="savingProfile()">
                  {{ t('dashboard.saveProfile') }}
                </app-button>
                <app-button type="button" variant="secondary" compact (click)="cancelEdit()">
                  {{ t('logout-confirm.cancel') }}
                </app-button>
              </div>
              @if (profileMessage()) {
                <p class="text-xs font-medium" [class]="profileError() ? 'text-red-600' : 'text-green-600'">
                  {{ profileMessage() }}
                </p>
              }
            </form>
          }
        </aside>

        <main>
          <div class="flex items-center justify-between">
            <h1 class="text-[28px] font-bold text-ink">{{ t('dashboard.heading') }}</h1>
            <app-button variant="primary" compact routerLink="/courses">
              + {{ t('dashboard.addCourse') }}
            </app-button>
          </div>

          <div class="mt-4">
            <app-error-slip [message]="errorMessage()" />
          </div>

          @if (loading()) {
            <p class="mt-8 text-sm text-muted">{{ t('dashboard.loading') }}</p>
          } @else if (courses().length === 0) {
            <div class="mt-8 rounded-xl border border-dashed border-line bg-surface p-8 text-center">
              <p class="text-base font-medium text-ink">{{ t('dashboard.emptyTitle') }}</p>
              <p class="mt-1 text-sm text-muted">{{ t('dashboard.emptyText') }}</p>
              <app-button variant="primary" compact routerLink="/courses" class="mt-4">
                {{ t('dashboard.browseCourses') }}
              </app-button>
            </div>
          } @else {
            <div class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
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
        </main>
      </div>
    </div>
  `,
})
export class DashboardPage {
  private authService = inject(AuthService);
  private session = inject(SessionService);
  private langService = inject(LangService);
  private router = inject(Router);
  private fb = inject(NonNullableFormBuilder);

  readonly user = this.session.user;
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly courses = signal<MyCourseResponse[]>([]);

  readonly editing = signal(false);
  readonly savingProfile = signal(false);
  readonly profileMessage = signal('');
  readonly profileError = signal(false);
  readonly avatarFailed = signal(false);

  readonly editForm = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
    profilePictureUrl: [''],
  });

  constructor() {
    if (this.session.user() === null) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadCourses();
  }

  readonly fullName = computed(() => {
    const u = this.user();
    return u ? `${u.firstName} ${u.lastName}`.trim() : '';
  });

  readonly initials = computed(() => {
    const u = this.user();
    if (!u) return '';
    return ((u.firstName?.[0] ?? '') + (u.lastName?.[0] ?? '')).toUpperCase();
  });

  readonly memberSince = computed(() => {
    const u = this.user();
    if (!u?.createdAt) return '';
    return new Date(u.createdAt).toLocaleDateString();
  });

  readonly age = computed(() => {
    const u = this.user();
    if (!u?.birthdate) return '';
    const birth = new Date(u.birthdate);
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const m = now.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) age--;
    return age + ' ' + this.t('dashboard.years');
  });

  readonly usernameError = computed(() => {
    const ctrl = this.editForm.controls.username;
    if (ctrl.hasError('required')) return this.t('error.required');
    if (ctrl.hasError('minlength') || ctrl.hasError('maxlength')) return this.t('dashboard.editUsernameHint');
    return '';
  });

  t(key: string): string {
    return this.langService.t(key);
  }

  memberTypeLabel(): string {
    const u = this.user();
    if (!u) return '';
    return u.accountType === 'INTERN' ? this.t('dashboard.type-intern') : this.t('dashboard.type-outsider');
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

  startEdit(): void {
    const u = this.user();
    if (!u) return;
    this.editForm.patchValue({
      username: u.username,
      profilePictureUrl: u.profilePictureUrl ?? '',
    });
    this.profileMessage.set('');
    this.profileError.set(false);
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.profileMessage.set('');
    this.profileError.set(false);
  }

  onAvatarError(): void {
    this.avatarFailed.set(true);
  }

  saveProfile(): void {
    this.profileMessage.set('');
    this.profileError.set(false);

    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.savingProfile.set(true);
    const { username, profilePictureUrl } = this.editForm.getRawValue();

    this.authService.updateProfile({
      username: username.trim().toLowerCase(),
      profilePictureUrl: profilePictureUrl || undefined,
    }).subscribe({
      next: (updated) => {
        this.session.setUser(updated);
        this.savingProfile.set(false);
        this.editing.set(false);
        this.profileMessage.set(this.t('dashboard.profileSaved'));
        this.profileError.set(false);
      },
      error: () => {
        this.savingProfile.set(false);
        this.profileMessage.set(this.t('dashboard.profileError'));
        this.profileError.set(true);
      },
    });
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
