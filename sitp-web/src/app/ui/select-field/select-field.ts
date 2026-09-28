import {
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  input,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

let nextId = 0;

@Component({
  selector: 'app-select-field',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SelectField),
      multi: true,
    },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col gap-1">
      <label [for]="id" class="text-sm font-medium text-ink">{{ label() }}</label>
      <select
        [id]="id"
        [value]="value"
        (change)="onChange($event)"
        (blur)="onBlur()"
        [disabled]="disabled()"
        class="w-full rounded-xl border bg-surface px-3 py-3 text-base text-ink
               focus:outline-none focus:ring-2 focus:ring-primary
               disabled:cursor-not-allowed disabled:opacity-50"
        [class.border-error]="blurred() && !!error()"
        [attr.aria-invalid]="blurred() && !!error() ? true : null"
      >
        @if (placeholder()) {
          <option value="" disabled [selected]="value === ''">{{ placeholder() }}</option>
        }
        @for (opt of options(); track opt.value) {
          <option [value]="opt.value" [selected]="value === opt.value">{{ opt.label }}</option>
        }
      </select>
      @if (blurred() && !!error()) {
        <p class="text-sm text-error">{{ error() }}</p>
      }
    </div>
  `,
})
export class SelectField implements ControlValueAccessor {
  label = input.required<string>();
  options = input.required<{ value: string; label: string }[]>();
  error = input<string>();
  placeholder = input<string>();

  readonly id = `sf-${nextId++}`;
  readonly disabled = signal(false);
  readonly blurred = signal(false);

  value = '';
  private onChangeFn: (v: string) => void = () => {};
  private onTouchedFn: () => void = () => {};

  writeValue(v: string | null): void {
    this.value = v ?? '';
  }
  registerOnChange(fn: (v: string) => void): void {
    this.onChangeFn = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouchedFn = fn;
  }
  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  onChange(e: Event): void {
    this.value = (e.target as HTMLSelectElement).value;
    this.onChangeFn(this.value);
  }

  onBlur(): void {
    this.blurred.set(true);
    this.onTouchedFn();
  }
}
