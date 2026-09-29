import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  booleanAttribute,
} from '@angular/core';

@Component({
  selector: 'app-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [type]="type()"
      [disabled]="loading()"
      [attr.aria-busy]="loading() ? true : null"
      class="focus:outline-none focus:ring-2 focus:ring-primary
             disabled:cursor-not-allowed disabled:opacity-50
             transition-[background-color,box-shadow,transform] duration-150"
      [class]="sizeClasses() + ' ' + variantClasses()"
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
      ? 'bg-primary text-white shadow-button hover:bg-primary-dark hover:shadow-button-hover hover:-translate-y-px'
      : 'border border-line bg-surface text-ink hover:bg-bg'
  );

  readonly compact = input(false, { transform: booleanAttribute });

  readonly sizeClasses = computed(() =>
    this.compact()
      ? 'w-auto rounded-full px-4 py-2 text-sm'
      : 'w-full rounded-xl py-3 text-base'
  );
}
