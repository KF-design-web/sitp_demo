import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';

@Component({
  selector: 'app-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [type]="type()"
      [disabled]="loading()"
      [attr.aria-busy]="loading() ? true : null"
      class="w-full rounded-lg py-3 text-base font-medium transition-colors
             focus:outline-none focus:ring-2 focus:ring-primary
             disabled:cursor-not-allowed disabled:opacity-50"
      [class]="variantClasses()"
    >
      @if (loading()) {
        {{ loadingLabel() }}
      } @else {
        <ng-content />
      }
    </button>
  `,
})
export class Button {
  type = input<'button' | 'submit'>('button');
  variant = input<'primary' | 'secondary'>('primary');
  loading = input(false);
  loadingLabel = input('');

  readonly variantClasses = computed(() =>
    this.variant() === 'primary'
      ? 'bg-primary text-white hover:bg-primary-dark'
      : 'border border-line bg-surface text-ink hover:bg-bg'
  );
}
