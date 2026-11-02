import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { PaymentsService } from '../../payments/payments.service';
import { PaymentProofRow } from '../../payments/payments.models';
import { formatXaf } from '../../courses/format-xaf';
import { ErrorSlip } from '../../ui/error-slip/error-slip';
import { Button } from '../../ui/button/button';
import { ConfirmModal } from '../../ui/confirm-modal/confirm-modal';
import { LangService } from '../../core/lang.service';
import { SessionService } from '../../core/session.service';

type Verdict = 'approve' | 'reject';

@Component({
  selector: 'app-payments-queue-page',
  imports: [ErrorSlip, Button, ConfirmModal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="mx-auto max-w-5xl px-8 py-8">
      <h1 class="text-[28px] font-bold text-ink">{{ t('payments.heading') }}</h1>
      <p class="mt-2 text-sm text-muted">{{ t('payments.subtitle') }}</p>

      @if (!isAssistant()) {
        <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 text-center shadow-card">
          <h2 class="text-lg font-semibold text-ink">{{ t('payments.denied-title') }}</h2>
          <p class="mt-2 text-sm text-muted">{{ t('payments.denied-text') }}</p>
        </div>
      } @else {
        @if (error()) {
          <div class="mt-8">
            <app-error-slip [message]="error()" />
          </div>
          <div class="mt-4 max-w-xs">
            <app-button variant="secondary" (click)="load()">
              {{ t('courses.retry') }}
            </app-button>
          </div>
        }

        @if (loading()) {
          <p class="mt-8 text-sm text-muted">{{ t('payments.loading') }}</p>
        }

        @if (!loading() && !error()) {
          @if (rows().length === 0) {
            <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 text-center shadow-card">
              <h2 class="text-lg font-semibold text-ink">{{ t('payments.empty-title') }}</h2>
              <p class="mt-2 text-sm text-muted">{{ t('payments.empty-text') }}</p>
            </div>
          } @else {
            <ul class="mt-8 space-y-3">
              @for (row of rows(); track row.id) {
                <li class="rounded-xl border border-line bg-surface p-6 shadow-card">
                  <div class="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <p class="text-sm font-semibold text-ink">{{ row.uploaderName }}</p>
                      <p class="text-xs text-muted">{{ row.uploaderUsername }}</p>
                    </div>
                    <span class="text-xs text-muted">{{ formatDate(row.createdAt) }}</span>
                  </div>

                  <div class="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
                    <div>
                      <p class="text-xs uppercase text-muted">{{ t('payments.col-course') }}</p>
                      <p class="mt-1 text-sm font-medium text-ink">{{ tText(row.courseTitle) }}</p>
                    </div>
                    <div>
                      <p class="text-xs uppercase text-muted">{{ t('payments.col-method') }}</p>
                      <p class="mt-1 text-sm font-medium text-ink">{{ row.paymentMethod }}</p>
                    </div>
                    <div>
                      <p class="text-xs uppercase text-muted">{{ t('payments.col-amount') }}</p>
                      <p class="mt-1 text-sm font-bold text-primary">{{ formatXaf(row.amount) }}</p>
                    </div>
                    <div>
                      <p class="text-xs uppercase text-muted">{{ t('payments.col-receipt') }}</p>
                      <a
                        [href]="paymentsService.receiptUrl(row.id)"
                        target="_blank"
                        rel="noopener"
                        class="mt-1 inline-block text-sm font-medium text-primary hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
                      >
                        {{ t('payments.view-receipt') }}
                      </a>
                    </div>
                  </div>

                  @if (verdictError() !== '') {
                    <div class="mt-4">
                      <app-error-slip [message]="verdictError()" />
                    </div>
                  }

                  <div class="mt-4 flex items-center gap-2">
                    <app-button
                      variant="primary"
                      compact
                      [disabled]="decidingId() !== null"
                      (click)="openAsk(row.id, 'approve')"
                    >
                      {{ t('payments.approve') }}
                    </app-button>
                    <app-button
                      variant="secondary"
                      compact
                      [disabled]="decidingId() !== null"
                      (click)="openAsk(row.id, 'reject')"
                    >
                      {{ t('payments.reject') }}
                    </app-button>
                  </div>
                </li>
              }
            </ul>
          }
        }

        @if (ask() !== null) {
          <app-confirm-modal
            [title]="ask()!.verdict === 'approve'
              ? t('payments.approve-confirm-title')
              : t('payments.reject-confirm-title')"
            [text]="ask()!.verdict === 'approve'
              ? t('payments.approve-confirm-text')
              : t('payments.reject-confirm-text')"
            [confirmLabel]="t('payments.confirm-go')"
            [cancelLabel]="t('payments.confirm-cancel')"
            (confirm)="decide()"
            (cancel)="closeAsk()"
          />
        }
      }
    </section>
  `,
})
export class PaymentsQueuePage {
  readonly paymentsService = inject(PaymentsService);
  private langService = inject(LangService);
  private session = inject(SessionService);

  readonly rows = signal<PaymentProofRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly ask = signal<{ id: number; verdict: Verdict } | null>(null);
  readonly decidingId = signal<number | null>(null);
  readonly verdictError = signal('');

  constructor() {
    this.load();
  }

  isAssistant(): boolean {
    const u = this.session.user();
    return u !== null && u.role === 'ADMIN_ASSISTANT';
  }

  load(): void {
    this.error.set('');
    this.loading.set(true);
    this.paymentsService.pending().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        this.error.set(
          err?.status === 403
            ? this.t('error.403-access')
            : this.t('error.unreachable'),
        );
      },
    });
  }

  openAsk(id: number, verdict: Verdict): void {
    this.verdictError.set('');
    this.ask.set({ id, verdict });
  }

  closeAsk(): void {
    this.ask.set(null);
  }

  decide(): void {
    const ask = this.ask();
    if (ask === null) {
      return;
    }
    this.decidingId.set(ask.id);
    this.verdictError.set('');
    const call = ask.verdict === 'approve'
      ? this.paymentsService.approve(ask.id)
      : this.paymentsService.reject(ask.id);
    call.subscribe({
      next: () => {
        this.decidingId.set(null);
        this.ask.set(null);
        this.load();
      },
      error: (err: { error?: { message?: string } }) => {
        this.decidingId.set(null);
        this.ask.set(null);
        this.verdictError.set(this.langService.tApiError(err?.error?.message));
      },
    });
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '';
    }
    const d = new Date(iso);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
      d.getDate(),
    ).padStart(2, '0')}`;
  }

  readonly formatXaf = formatXaf;

  tText(en: string): string {
    return this.langService.tText(en);
  }

  t(key: string): string {
    return this.langService.t(key);
  }
}
