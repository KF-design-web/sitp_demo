import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { COUNTRIES } from '../../auth/countries';
import { TextField } from '../../ui/text-field/text-field';
import { SelectField } from '../../ui/select-field/select-field';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { CaptchaField } from '../../ui/captcha-field/captcha-field';
import { VerifyEmailPage } from '../verify-email/verify-email';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, TextField, SelectField, Button, ErrorSlip, CaptchaField, VerifyEmailPage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-3xl rounded-xl border border-line bg-surface p-8 shadow-card">
      <h1 class="text-[28px] font-bold text-ink">{{ t('register.heading') }}</h1>

      <div class="mt-6">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (!submitted()) {
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-6 flex flex-col gap-6">
          <fieldset class="rounded-xl border border-line p-4">
            <legend class="px-2 text-sm font-semibold text-primary">
              {{ t('register.profile.section') }}
            </legend>
            <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
              <app-text-field formControlName="firstName" [label]="t('register.firstName')" [error]="firstNameError()" />
              <app-text-field formControlName="lastName" [label]="t('register.lastName')" [error]="lastNameError()" />
              <app-text-field formControlName="phoneNumber" type="text" [label]="t('register.phoneNumber')" />
              <app-select-field
                formControlName="gender"
                [label]="t('register.gender')"
                [options]="genderOptions"
                [placeholder]="t('register.gender-placeholder')"
              />
              <app-select-field
                formControlName="country"
                [label]="t('register.country')"
                [options]="countryOptions"
                [placeholder]="t('register.country-placeholder')"
              />
              <app-text-field formControlName="address" type="text" [label]="t('register.address')" />
              <app-text-field formControlName="email" type="email" [label]="t('register.email')" [hint]="t('register.email-hint')" [error]="emailError()" />
              <app-text-field formControlName="username" type="text" [label]="t('register.username')" [hint]="t('register.username-hint')" [error]="usernameError()" />
            </div>
          </fieldset>

          <fieldset class="rounded-xl border border-line p-4">
            <legend class="px-2 text-sm font-semibold text-primary">
              {{ t('register.security.section') }}
            </legend>
            <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
              <app-text-field formControlName="password" type="password" [label]="t('register.password')" [hint]="t('register.password-hint')" [error]="passwordError()" />
              <app-text-field formControlName="passwordConfirm" type="password" [label]="t('register.password-confirm')" [error]="passwordConfirmError()" />
            </div>
            <div class="mt-4">
              <app-captcha-field [invalid]="captchaInvalid()" (resolved)="onCaptcha($event)" />
            </div>
          </fieldset>

          <div>
            <app-button type="submit" [loading]="submitting()" [loadingLabel]="t('register.loading')">
              {{ t('register.submit') }}
            </app-button>
          </div>
        </form>

        <p class="mt-6 text-sm text-muted">
          {{ t('register.have-account') }}
          <a routerLink="/login" class="font-medium text-primary hover:underline">
            {{ t('register.sign-in-link') }}
          </a>
        </p>
      } @else {
        <app-verify-email-page [username]="submittedUsername()" class="mt-6 block" />
      }
    </div>
  `,
})
export class RegisterPage {
  private captchaField = viewChild.required(CaptchaField);

  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private session = inject(SessionService);
  private langService = inject(LangService);
  private router = inject(Router);

  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly submittedUsername = signal('');
  readonly errorMessage = signal('');
  readonly captchaInvalid = signal(false);

  captchaPair: { id: string; answer: string } = { id: '', answer: '' };

  readonly countryOptions = COUNTRIES.map((c) => ({ value: c, label: c }));
  readonly genderOptions = [
    { value: 'MALE', label: this.t('register.gender-MALE') },
    { value: 'FEMALE', label: this.t('register.gender-FEMALE') },
  ];
  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phoneNumber: [''],
    gender: [''],
    country: [''],
    address: [''],
    email: ['', [Validators.required, Validators.email]],
    username: ['', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(30),
      Validators.pattern('^[a-zA-Z0-9._-]+$'),
    ]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    passwordConfirm: ['', [Validators.required]],
  });

  private readonly formValues = toSignal(this.form.valueChanges, { initialValue: undefined });

  private fieldError(name: 'email' | 'username' | 'firstName' | 'lastName' | 'password' | 'passwordConfirm'): string {
    this.formValues();
    const c = this.form.controls[name];
    if (!c.invalid) {
      return '';
    }
    if (c.hasError('required')) {
      return this.t('error.required');
    }
    if (name === 'email' && c.hasError('email')) {
      return this.t('error.email-invalid');
    }
    if (name === 'username' && c.hasError('pattern')) {
      return this.t('register.username-pattern');
    }
    if (name === 'username' && (c.hasError('minlength') || c.hasError('maxlength'))) {
      return this.t('register.username-hint');
    }
    if (name === 'password' && c.hasError('minlength')) {
      return this.t('register.password-hint');
    }
    return '';
  }

  readonly emailError = computed(() => this.fieldError('email'));
  readonly usernameError = computed(() => this.fieldError('username'));
  readonly firstNameError = computed(() => this.fieldError('firstName'));
  readonly lastNameError = computed(() => this.fieldError('lastName'));
  readonly passwordError = computed(() => this.fieldError('password'));
  readonly passwordConfirmError = computed(() => this.fieldError('passwordConfirm'));

  t(key: string): string {
    return this.langService.t(key);
  }

  constructor() {
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
    const v = this.form.getRawValue();
    const gender = v.gender === 'MALE' || v.gender === 'FEMALE' ? (v.gender as 'MALE' | 'FEMALE') : null;

    this.submitting.set(true);
    this.authService.register({
      email: v.email.trim().toLowerCase(),
      username: v.username.trim().toLowerCase(),
      password: v.password,
      passwordConfirm: v.passwordConfirm,
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      phoneNumber: v.phoneNumber || undefined,
      address: v.address || undefined,
      country: v.country || undefined,
      gender,
      captchaId: this.captchaPair.id,
      captchaAnswer: this.captchaPair.answer,
    }).subscribe({
      next: () => {
        this.submittedUsername.set(v.username.trim().toLowerCase());
        this.submitted.set(true);
        this.submitting.set(false);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.submitting.set(false);
        if (err?.status === 409) {
          this.errorMessage.set(
            (err.error?.message ?? '').includes('username')
              ? this.t('register.409-username')
              : this.t('error.409-email'),
          );
        } else if (err?.status === 400) {
          const msg = err.error?.message ?? '';
          if (msg.includes('Captcha')) {
            this.captchaInvalid.set(true);
            this.captchaField().refresh();
          } else if (msg.includes('match')) {
            this.errorMessage.set(this.t('register.password-mismatch'));
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
