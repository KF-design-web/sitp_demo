import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { TextField } from '../../ui/text-field/text-field';
import { SelectField } from '../../ui/select-field/select-field';
import { Button } from '../../ui/button/button';
import { ErrorSlip } from '../../ui/error-slip/error-slip';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, TextField, SelectField, Button, ErrorSlip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 shadow-card">
      <h1 class="text-[28px] font-bold text-ink">{{ t('register.heading') }}</h1>

      <div class="mt-6">
        <app-error-slip [message]="errorMessage()" />
      </div>

      @if (chaining()) {
        <p class="mt-4 text-sm text-success">{{ t('register.success-auto-login') }}</p>
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-6 flex flex-col gap-4">
        <app-text-field
          formControlName="email"
          type="email"
          [label]="t('register.email')"
          [hint]="t('register.email-hint')"
          [error]="emailError()"
        />
        <app-text-field
          formControlName="password"
          type="password"
          [label]="t('register.password')"
          [hint]="t('register.password-hint')"
          [error]="passwordError()"
        />
        <app-select-field
          formControlName="accountType"
          [label]="t('register.account-type')"
          [options]="accountTypeOptions"
          [error]="accountTypeError()"
        />
        @if (isIntern()) {
          <app-select-field
            formControlName="track"
            [label]="t('register.track')"
            [options]="trackOptions"
            [error]="trackError()"
          />
        }
        <app-button type="submit" [loading]="submitting()" [loadingLabel]="t('register.loading')">
          {{ t('register.submit') }}
        </app-button>
      </form>

      <p class="mt-6 text-sm text-muted">
        {{ t('register.have-account') }}
        <a routerLink="/login" class="font-medium text-primary hover:underline">
          {{ t('register.sign-in-link') }}
        </a>
      </p>
    </div>
  `,
})
export class RegisterPage {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private session = inject(SessionService);
  private langService = inject(LangService);
  private router = inject(Router);

  readonly submitting = signal(false);
  readonly chaining = signal(false);
  readonly errorMessage = signal('');

  readonly accountTypeOptions = [
    { value: 'INTERN', label: this.t('register.account-type-INTERN') },
    { value: 'OUTSIDER', label: this.t('register.account-type-OUTSIDER') },
  ];
  readonly trackOptions = [
    { value: 'ACADEMIC', label: this.t('register.track-ACADEMIC') },
    { value: 'PROFESSIONAL', label: this.t('register.track-PROFESSIONAL') },
  ];

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    accountType: ['', [Validators.required]],
    track: [{ value: '', disabled: false }, [Validators.required]],
  });

  private readonly formValues = toSignal(this.form.valueChanges, { initialValue: undefined });

  readonly isIntern = computed(() => {
    this.formValues();
    return this.form.controls.accountType.value === 'INTERN';
  });

  private fieldError(name: 'email' | 'password' | 'accountType' | 'track'): string {
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
    if (name === 'password' && c.hasError('minlength')) {
      return this.t('register.password-hint');
    }
    return '';
  }

  readonly emailError = computed(() => this.fieldError('email'));
  readonly passwordError = computed(() => this.fieldError('password'));
  readonly accountTypeError = computed(() => this.fieldError('accountType'));
  readonly trackError = computed(() => this.fieldError('track'));

  t(key: string): string {
    return this.langService.t(key);
  }

  constructor() {

    this.form.controls.accountType.valueChanges.subscribe((v) => {
      if (v === 'OUTSIDER') {
        this.form.controls.track.reset('');
        this.form.controls.track.disable();
      } else {
        this.form.controls.track.enable();
      }
    });
  }

  onSubmit(): void {
    this.errorMessage.set('');
    this.chaining.set(false);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password, accountType } = this.form.getRawValue();
    const track = this.isIntern() ? this.form.controls.track.value : null;

    this.submitting.set(true);
    this.authService.register({ email, password, accountType: accountType as 'INTERN' | 'OUTSIDER', track }).subscribe({
      next: () => {
        this.chaining.set(true);
        this.authService.login({ email, password }).subscribe({
          next: (res) => {
            this.session.setUser(res.user);
            this.router.navigate(['/courses']);
          },
          error: () => {
            this.submitting.set(false);
            this.errorMessage.set(this.t('error.401-login'));
            this.router.navigate(['/login']);
          },
        });
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.submitting.set(false);
        if (err?.status === 409) {
          this.errorMessage.set(this.t('error.409-email'));
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
