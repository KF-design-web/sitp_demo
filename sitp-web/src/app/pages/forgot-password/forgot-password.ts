import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { LangService } from '../../core/lang.service';
import { TextField } from '../../ui/text-field/text-field';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink, TextField, Button, ErrorSlip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 shadow-card">
      <h1 class="text-[28px] font-bold text-ink">
        {{ sent() ? t('reset.heading') : t('forgot.heading') }}
      </h1>

      <div class="mt-6">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (done()) {
        <p class="mt-4 text-sm font-medium text-success">{{ t('reset.success-then-login') }}</p>
        <div class="mt-4">
          <a
            routerLink="/login"
            class="font-medium text-primary underline-offset-2 hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
          >{{ t('login.submit') }}</a>
        </div>
      } @else if (!sent()) {
        <p class="mt-2 text-sm text-muted">{{ t('forgot.text') }}</p>
        <form [formGroup]="askForm" (ngSubmit)="onAsk()" class="mt-6 flex flex-col gap-4">
          <app-text-field
            formControlName="email"
            type="email"
            [label]="t('forgot.email')"
            [error]="emailError()"
          />
          <div>
            <app-button type="submit" [loading]="submitting()" [loadingLabel]="t('forgot.loading')">
              {{ t('forgot.submit') }}
            </app-button>
          </div>
        </form>
        <p class="mt-6 text-sm text-muted">
          <a
            routerLink="/login"
            class="font-medium text-primary hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
          >{{ t('login.submit') }}</a>
        </p>
      } @else {
        <p class="mt-2 text-sm font-medium text-ink">{{ t('forgot.sent-title') }}</p>
        <p class="mt-1 text-sm text-muted">{{ t('forgot.sent-text') }}</p>
        <form [formGroup]="resetForm" (ngSubmit)="onReset()" class="mt-6 flex flex-col gap-4">
          <app-text-field
            formControlName="username"
            type="text"
            [label]="t('reset.username')"
            [error]="usernameError()"
          />
          <app-text-field
            formControlName="code"
            type="text"
            [label]="t('reset.code')"
            [hint]="t('verify.code-hint')"
            [error]="codeError()"
          />
          <app-text-field
            formControlName="newPassword"
            type="password"
            [label]="t('reset.new-password')"
            [hint]="t('register.password-hint')"
            [error]="newPasswordError()"
          />
          <app-text-field
            formControlName="newPasswordConfirm"
            type="password"
            [label]="t('reset.new-password-confirm')"
            [error]="confirmError()"
          />
          <div>
            <app-button type="submit" [loading]="submitting()" [loadingLabel]="t('reset.loading')">
              {{ t('reset.submit') }}
            </app-button>
          </div>
        </form>
      }
    </div>
  `,
})
export class ForgotPasswordPage {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private langService = inject(LangService);

  readonly submitting = signal(false);
  readonly sent = signal(false);
  readonly done = signal(false);
  readonly errorMessage = signal('');

  readonly askForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  readonly resetForm = this.fb.group({
    username: ['', [Validators.required]],
    code: ['', [Validators.required, Validators.pattern('^\\d{6}$')]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    newPasswordConfirm: ['', [Validators.required]],
  });

  private readonly askValues = toSignal(this.askForm.valueChanges, { initialValue: undefined });
  private readonly resetValues = toSignal(this.resetForm.valueChanges, { initialValue: undefined });

  readonly emailError = computed(() => {
    this.askValues();
    const c = this.askForm.controls.email;
    if (!c.invalid) return '';
    if (c.hasError('required')) return this.t('error.required');
    return this.t('error.email-invalid');
  });

  readonly usernameError = computed(() => {
    this.resetValues();
    return this.resetForm.controls.username.invalid ? this.t('error.required') : '';
  });
  readonly codeError = computed(() => {
    this.resetValues();
    return this.resetForm.controls.code.invalid ? this.t('verify.code-hint') : '';
  });
  readonly newPasswordError = computed(() => {
    this.resetValues();
    return this.resetForm.controls.newPassword.invalid ? this.t('register.password-hint') : '';
  });
  readonly confirmError = computed(() => {
    this.resetValues();
    return this.resetForm.controls.newPasswordConfirm.invalid ? this.t('error.required') : '';
  });

  t(key: string): string {
    return this.langService.t(key);
  }

  onAsk(): void {
    this.errorMessage.set('');
    if (this.askForm.invalid) {
      this.askForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const { email } = this.askForm.getRawValue();
    this.authService.forgotPassword(email.trim().toLowerCase()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.sent.set(true);
      },
      error: (err: { status?: number }) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.status === 0 ? this.t('error.unreachable') : this.t('error.fallback'));
      },
    });
  }

  onReset(): void {
    this.errorMessage.set('');
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const { username, code, newPassword, newPasswordConfirm } = this.resetForm.getRawValue();
    this.authService.resetPassword({
      username: username.trim().toLowerCase(),
      code,
      newPassword,
      newPasswordConfirm,
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.done.set(true);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.submitting.set(false);
        if (err?.status === 400) {
          const msg = err.error?.message ?? '';
          this.errorMessage.set(msg.includes('match') ? this.t('register.password-mismatch') : this.t('error.400-code'));
        } else if (err?.status === 0) {
          this.errorMessage.set(this.t('error.unreachable'));
        } else {
          this.errorMessage.set(err.error?.message ?? this.t('error.fallback'));
        }
      },
    });
  }
}
