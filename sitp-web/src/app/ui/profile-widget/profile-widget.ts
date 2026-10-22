import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { Button } from '../button/button';
import { TextField } from '../text-field/text-field';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-profile-widget',
  imports: [Button, TextField, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'close()',
  },
  template: `
    <div class="relative" (click)="$event.stopPropagation()">
      <button
        type="button"
        (click)="toggle()"
        aria-label="Profile"
        class="flex h-10 w-10 items-center justify-center overflow-hidden rounded-full bg-primary/10
               ring-2 ring-primary/20 transition hover:ring-primary/50 focus:outline-none focus:ring-primary"
      >
        @if (user()?.profilePictureUrl && !avatarFailed()) {
          <img
            [src]="user()!.profilePictureUrl"
            alt=""
            class="h-full w-full object-cover"
            (error)="avatarFailed.set(true)"
          />
        } @else {
          <span class="text-sm font-bold text-primary">{{ initials() }}</span>
        }
      </button>

      @if (open()) {
        <div
          class="absolute right-0 top-12 z-40 w-72 rounded-xl border border-line bg-surface p-5 shadow-card"
        >
          <div class="flex items-center gap-3">
            @if (user()?.profilePictureUrl && !avatarFailed()) {
              <img
                [src]="user()!.profilePictureUrl"
                alt=""
                class="h-12 w-12 shrink-0 rounded-full object-cover ring-2 ring-primary/20"
              />
            } @else {
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary/10">
                <span class="text-base font-bold text-primary">{{ initials() }}</span>
              </div>
            }
            <div class="min-w-0">
              <p class="truncate text-sm font-semibold text-ink">{{ fullName() }}</p>
              <p class="truncate text-xs text-muted">{{ '@' + (user()?.username ?? '') }}</p>
            </div>
          </div>

          <span class="mt-3 inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
            {{ badgeLabel() }}
          </span>

          <dl class="mt-4 space-y-2 text-xs">
            <div class="flex justify-between gap-2">
              <dt class="text-muted">{{ t('dashboard.memberSince') }}</dt>
              <dd class="font-medium text-ink">{{ memberSince() }}</dd>
            </div>
            @if (user()?.email) {
              <div class="flex justify-between gap-2">
                <dt class="text-muted">{{ t('dashboard.email') }}</dt>
                <dd class="ml-2 truncate font-medium text-ink">{{ user()?.email }}</dd>
              </div>
            }
          </dl>

          @if (!editing()) {
            <div class="mt-4 border-t border-line pt-3">
              <button
                type="button"
                class="text-sm font-medium text-primary hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
                (click)="startEdit()"
              >
                {{ t('dashboard.editProfile') }}
              </button>
            </div>
          } @else {
            <form [formGroup]="editForm" (ngSubmit)="saveProfile()" class="mt-4 space-y-3 border-t border-line pt-3">
              <app-text-field
                formControlName="username"
                type="text"
                [label]="t('dashboard.editUsername')"
                [hint]="t('dashboard.editUsernameHint')"
                [error]="t('error.required')"
              />
              <app-text-field
                formControlName="profilePictureUrl"
                type="text"
                [label]="t('dashboard.editPictureUrl')"
                [hint]="t('dashboard.editPictureUrlHint')"
              />
              <div class="flex gap-2">
                <app-button type="submit" compact [loading]="saving()">
                  {{ t('dashboard.saveProfile') }}
                </app-button>
                <app-button type="button" variant="secondary" compact (click)="cancelEdit()">
                  {{ t('logout-confirm.cancel') }}
                </app-button>
              </div>
              @if (message()) {
                <p class="text-xs font-medium" [class]="error() ? 'text-red-600' : 'text-green-600'">
                  {{ message() }}
                </p>
              }
            </form>
          }
        </div>
      }
    </div>
  `,
})
export class ProfileWidget {
  private authService = inject(AuthService);
  private session = inject(SessionService);
  private langService = inject(LangService);
  private fb = inject(NonNullableFormBuilder);

  readonly user = this.session.user;

  readonly open = signal(false);
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly message = signal('');
  readonly error = signal(false);
  readonly avatarFailed = signal(false);

  readonly editForm = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
    profilePictureUrl: [''],
  });

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

  badgeLabel(): string {
    const u = this.user();
    if (!u) return '';
    if (u.role === 'TRAINER') return this.t('trainer.roleBadge');
    return u.accountType === 'INTERN'
      ? this.t('dashboard.type-intern')
      : this.t('dashboard.type-outsider');
  }

  t(key: string): string {
    return this.langService.t(key);
  }

  toggle(): void {
    const next = !this.open();
    this.open.set(next);
    if (!next) {
      this.editing.set(false);
      this.message.set('');
    }
  }

  close(): void {
    this.open.set(false);
    this.editing.set(false);
    this.message.set('');
  }

  startEdit(): void {
    const u = this.user();
    if (!u) return;
    this.editForm.patchValue({
      username: u.username,
      profilePictureUrl: u.profilePictureUrl ?? '',
    });
    this.message.set('');
    this.error.set(false);
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.message.set('');
    this.error.set(false);
  }

  saveProfile(): void {
    this.message.set('');
    this.error.set(false);

    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const { username, profilePictureUrl } = this.editForm.getRawValue();

    this.authService.updateProfile({
      username: username.trim().toLowerCase(),
      profilePictureUrl: profilePictureUrl || undefined,
    }).subscribe({
      next: (updated) => {
        this.session.setUser(updated);
        this.saving.set(false);
        this.editing.set(false);
        this.message.set(this.t('dashboard.profileSaved'));
      },
      error: () => {
        this.saving.set(false);
        this.message.set(this.t('dashboard.profileError'));
        this.error.set(true);
      },
    });
  }
}
