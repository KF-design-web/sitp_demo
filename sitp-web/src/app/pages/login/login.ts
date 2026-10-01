import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { TextField } from '../../ui/text-field/text-field';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { CaptchaField } from '../../ui/captcha-field/captcha-field';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, TextField, Button, ErrorSlip, CaptchaField],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 shadow-card">
      <h1 class="text-[28px] font-bold text-ink">{{ t('login.heading') }}</h1>

      <div class="mt-6">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (verifiedNow()) {
        <p class="mt-4 text-sm font-medium text-success">{{ t('verify.success-then-login') }}</p>
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-6 flex flex-col gap-4">
        <app-text-field
          formControlName="username"
          type="text"
          [label]="t('login.username')"
          [hint]="t('login.username-hint')"
          [error]="usernameError()"
        />
        <app-text-field
          formControlName="password"
          type="password"
          [label]="t('register.password')"
        />
        <app-captcha-field [invalid]="captchaInvalid()" (resolved)="onCaptcha($event)" />
        <div>
          <app-button type="submit" [loading]="submitting()" [loadingLabel]="t('login.loading')">
            {{ t('login.submit') }}
          </app-button>
        </div>
      </form>

      <p class="mt-4 text-sm">
        <a
          routerLink="/forgot-password"
          class="font-medium text-primary underline-offset-2 hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
        >{{ t('login.forgot-password') }}</a>
      </p>

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
  private captchaField = viewChild.required(CaptchaField);
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private session = inject(SessionService);
  private langService = inject(LangService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly captchaInvalid = signal(false);
  readonly verifiedNow = signal(false);

  captchaPair: { id: string; answer: string } = { id: '', answer: '' };

  constructor() {
    this.route.queryParamMap.subscribe((q) => {
      if (q.get('verified')) {
        this.verifiedNow.set(true);
      }
    });
  }

  readonly form = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });


  private readonly formValues = toSignal(this.form.valueChanges, { initialValue: undefined });

  readonly usernameError = computed(() => {
    this.formValues();
    return this.form.controls.username.invalid ? this.t('error.required') : '';
  });

  t(key: string): string {
    return this.langService.t(key);
  }

  onCaptcha(pair: { id: string; answer: string }): void {
    this.captchaPair = pair;
  }

  onSubmit(): void {
    this.errorMessage.set('');
    this.captchaInvalid.set(false);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.captchaPair.id || !this.captchaPair.answer) {
      this.captchaInvalid.set(true);
      return;
    }
    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();

    this.authService.login({
      username: username.trim().toLowerCase(),
      password,
      captchaId: this.captchaPair.id,
      captchaAnswer: this.captchaPair.answer,
    }).subscribe({
      next: (res) => {
        this.session.setUser(res.user);
        this.router.navigate(['/courses']);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.submitting.set(false);
        if (err?.status === 401) {
          this.errorMessage.set(this.t('error.401-login'));
          this.captchaField().refresh();
        } else if (err?.status === 403) {
          this.errorMessage.set(this.t('login.disabled-account'));
        } else if (err?.status === 400) {
          const msg = err.error?.message ?? '';
          if (msg.includes('Captcha')) {
            this.captchaInvalid.set(true);
            this.captchaField().refresh();
          } else {
            this.errorMessage.set(msg || this.t('error.fallback'));
          }
        } else if (err?.status === 0) {
          this.errorMessage.set(this.t('error.unreachable'));
        } else {
          this.errorMessage.set(this.t('error.fallback'));
        }
      },
    });
  }
}
