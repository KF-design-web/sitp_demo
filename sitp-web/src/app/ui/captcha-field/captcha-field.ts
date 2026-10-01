import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../../core/api-url';
import { CaptchaChallenge } from '../../auth/auth.models';
import { LangService } from '../../core/lang.service';

@Component({
  selector: 'app-captcha-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col gap-1">
      <span class="text-sm font-medium text-ink">{{ t('login.captcha') }}</span>
      <div class="flex items-center gap-2">
        <span
          class="flex-1 select-none rounded-xl border border-line bg-bg px-3 py-3 text-base font-semibold tracking-wide text-ink"
        >
          {{ challenge()?.question ?? '…' }}
        </span>
        <input
          type="text"
          inputmode="numeric"
          [placeholder]="t('register.captcha-placeholder')"
          [value]="answer()"
          (input)="onAnswer($event)"
          class="w-28 rounded-full border border-line bg-surface px-4 py-2 text-sm text-ink placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-primary"
          [class.border-error]="invalid()"
          [attr.aria-invalid]="invalid() ? true : null"
        />
      </div>
      @if (invalid()) {
        <p class="text-sm text-error">{{ t('register.captcha-invalid') }}</p>
      }
    </div>
  `,
})
export class CaptchaField {
  private http = inject(HttpClient);
  private langService = inject(LangService);

  invalid = input<boolean>(false);

  resolved = output<{ id: string; answer: string }>();

  readonly challenge = signal<CaptchaChallenge | null>(null);
  readonly answer = signal('');

  constructor() {
    this.refresh();
  }

  t(key: string): string {
    return this.langService.t(key);
  }

  refresh(): void {
    this.answer.set('');
    this.http.get<CaptchaChallenge>(`${API_URL}/auth/captcha`).subscribe({
      next: (c) => this.challenge.set(c),
      error: () => this.challenge.set(null),
    });
  }

  private emit(): void {
    const c = this.challenge();
    if (c) {
      this.resolved.emit({ id: String(c.id), answer: this.answer() });
    }
  }

  onAnswer(e: Event): void {
    this.answer.set((e.target as HTMLInputElement).value);
    this.emit();
  }
}
