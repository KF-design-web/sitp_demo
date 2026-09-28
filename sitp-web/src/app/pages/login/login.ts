import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { TextField } from '../../ui/text-field/text-field';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, TextField, Button, ErrorSlip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 shadow-card">
      <h1 class="text-[28px] font-bold text-ink">{{ t('login.heading') }}</h1>

      <div class="mt-6">
        <app-error-slip [message]="errorMessage()" />
      </div>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-6 flex flex-col gap-4">
        <app-text-field
          formControlName="email"
          type="email"
          [label]="t('register.email')"
          [error]="emailError()"
        />
        <app-text-field
          formControlName="password"
          type="password"
          [label]="t('register.password')"
        />
        <app-button type="submit" [loading]="submitting()" [loadingLabel]="t('login.loading')">
          {{ t('login.submit') }}
        </app-button>
      </form>

      <p class="mt-6 text-sm text-muted">
        {{ t('login.no-account-yet') }}
        <a routerLink="/register" class="font-medium text-primary hover:underline">
          {{ t('login.create-one') }}
        </a>
      </p>
    </div>
  `,
})
export class LoginPage {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private session = inject(SessionService);
  private langService = inject(LangService);
  private router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });


  private readonly formValues = toSignal(this.form.valueChanges, { initialValue: undefined });

  readonly emailError = computed(() => {
    this.formValues();
    const email = this.form.controls.email;
    return email.invalid ? this.t('error.email-invalid') : '';
  });

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
    const { email, password } = this.form.getRawValue();

    this.authService.login({ email, password }).subscribe({
      next: (res) => {
        this.session.setUser(res.user);
        this.router.navigate(['/welcome']);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.submitting.set(false);
        if (err?.status === 401) {
          this.errorMessage.set(this.t('error.401-login'));
        } else if (err?.status === 400) {
          this.errorMessage.set(err.error?.message ?? this.t('error.fallback'));
        } else if (err?.status === 0) {
          this.errorMessage.set(this.t('error.unreachable'));
        } else {
          this.errorMessage.set(this.t('error.fallback'));
        }
      },
    });
  }
}
