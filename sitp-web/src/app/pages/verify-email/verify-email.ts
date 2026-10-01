import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { LangService } from '../../core/lang.service';
import { TextField } from '../../ui/text-field/text-field';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';

@Component({
  selector: 'app-verify-email-page',
  imports: [ReactiveFormsModule, RouterLink, TextField, Button, ErrorSlip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <h2 class="text-xl font-bold text-ink">{{ t('verify.heading') }}</h2>

      <div class="mt-4">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (verified()) {
        <p class="mt-4 text-sm font-medium text-success">{{ t('verify.success-then-login') }}</p>
        <div class="mt-4">
          <a
            routerLink="/login"
            [queryParams]="{ verified: '1' }"
            class="font-medium text-primary underline-offset-2 hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
          >{{ t('login.submit') }}</a>
        </div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-6 flex flex-col gap-4">
          @if (!username()) {
            <app-text-field
              formControlName="username"
              type="text"
              [label]="t('login.username')"
              [error]="usernameError()"
            />
          }
          <app-text-field
            formControlName="code"
            type="text"
            [label]="t('verify.code')"
            [hint]="t('verify.code-hint')"
            [error]="codeError()"
          />
          <div>
            <app-button type="submit" [loading]="submitting()" [loadingLabel]="t('verify.loading')">
              {{ t('verify.submit') }}
            </app-button>
          </div>
        </form>
      }
    </div>
  `,
})
export class VerifyEmailPage {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private langService = inject(LangService);

  username = input<string>('');

  readonly submitting = signal(false);
  readonly verified = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.fb.group({
    username: [this.username(), [Validators.required]],
    code: ['', [Validators.required, Validators.pattern('^\\d{6}$')]],
  });

  private readonly formValues = toSignal(this.form.valueChanges, { initialValue: undefined });

  readonly usernameError = computed(() => this.fieldError('username', this.t('error.required')));
  readonly codeError = computed(() => this.fieldError('code', this.t('verify.code-hint')));

  constructor() {
    effect(() => {
      const u = this.username();
      if (u) {
        this.form.controls.username.setValue(u);
      }
    });
  }

  private fieldError(name: 'username' | 'code', message: string): string {
    this.formValues();
    const c = this.form.controls[name];
    return c.invalid ? message : '';
  }

  t(key: string): string {
    return this.langService.t(key);
  }

  onSubmit(): void {
    this.errorMessage.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const { username, code } = this.form.getRawValue();
    this.authService.verifyEmail(username.trim().toLowerCase(), code).subscribe({
      next: () => {
        this.submitting.set(false);
        this.verified.set(true);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.submitting.set(false);
        if (err?.status === 400) {
          this.errorMessage.set(this.t('error.400-code'));
        } else if (err?.status === 0) {
          this.errorMessage.set(this.t('error.unreachable'));
        } else {
          this.errorMessage.set(err.error?.message ?? this.t('error.fallback'));
        }
      },
    });
  }
}
