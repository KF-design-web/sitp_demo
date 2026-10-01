import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 p-4"
      (click)="cancel.emit()"
    >
      <section
        role="alertdialog"
        aria-modal="true"
        class="w-full max-w-sm rounded-xl border border-line bg-surface p-6 shadow-card"
        (click)="$event.stopPropagation()"
      >
        <h2 class="text-lg font-bold text-ink">{{ title() }}</h2>
        <p class="mt-2 text-sm text-muted">{{ text() }}</p>
        <div class="mt-6 flex justify-end gap-3">
          <button
            type="button"
            class="rounded-xl border border-line px-4 py-2 text-sm font-medium text-ink transition-colors hover:bg-bg focus:outline-none focus:ring-2 focus:ring-primary"
            (click)="cancel.emit()"
          >
            {{ cancelLabel() }}
          </button>
          <button
            type="button"
            class="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white shadow-button transition-all hover:-translate-y-px hover:bg-primary-dark hover:shadow-button-hover focus:outline-none focus:ring-2 focus:ring-primary"
            (click)="confirm.emit()"
          >
            {{ confirmLabel() }}
          </button>
        </div>
      </section>
    </div>
  `,
})
export class ConfirmModal {
  title = input.required<string>();
  text = input.required<string>();
  confirmLabel = input.required<string>();
  cancelLabel = input.required<string>();

  confirm = output<void>();
  cancel = output<void>();
}
