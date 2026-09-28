import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
  output,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Eye, EyeOff, LucideAngularModule } from 'lucide-angular';

let nextId = 0;

@Component({
  selector: 'app-text-field',
  imports: [LucideAngularModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TextField),
      multi: true,
    },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col gap-1">
      <label [for]="id" class="text-sm font-medium text-ink">{{ label() }}</label>
      <div class="relative">
        <input
          [id]="id"
          [type]="reveal() ? 'text' : type()"
          [value]="value"
          (input)="onInput($event)"
          (blur)="onBlur()"
          [disabled]="disabled()"
          [attr.aria-invalid]="showInvalid() ? true : null"
          [attr.aria-describedby]="showInvalid() ? id + '-error' : null"
          class="w-full rounded-xl border bg-surface px-3 py-3 text-base text-ink
                 placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-primary
                 disabled:cursor-not-allowed disabled:opacity-50"
          [class.border-error]="showInvalid()"
          [class.pr-10]="type() === 'password'"
        />
        @if (type() === 'password') {
          <button
            type="button"
            (click)="toggleReveal()"
            class="absolute inset-y-0 right-3 my-auto flex h-6 w-6 items-center justify-center text-muted hover:text-ink"
            [attr.aria-label]="reveal() ? 'Hide password' : 'Show password'"
          >
            <lucide-icon [img]="reveal() ? eyeOff : eye" [size]="16" />
          </button>
        }
      </div>
      @if (hint() && !showInvalid()) {
        <p class="text-sm text-muted">{{ hint() }}</p>
      }
      @if (showInvalid()) {
        <p [id]="id + '-error'" class="text-sm text-error">{{ error() }}</p>
      }
    </div>
  `,
})
export class TextField implements ControlValueAccessor {
  label = input.required<string>();
  type = input<'text' | 'password' | 'email'>('text');
  hint = input<string>();
  error = input<string>();
  touched = output<void>();

  readonly id = `tf-${nextId++}`;

  readonly eye = Eye;
  readonly eyeOff = EyeOff;
  readonly reveal = signal(false);
  readonly blurred = signal(false);
  readonly disabled = signal(false);

  readonly showInvalid = computed(() => this.blurred() && !!this.error());

  value = '';
  private onChange: (v: string) => void = () => {};
  private onTouched: () => void = () => {};

  toggleReveal(): void {
    this.reveal.update((r) => !r);
  }

  writeValue(v: string | null): void {
    this.value = v ?? '';
  }
  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  onInput(e: Event): void {
    this.value = (e.target as HTMLInputElement).value;
    this.onChange(this.value);
  }

  onBlur(): void {
    this.blurred.set(true);
    this.onTouched();
    this.touched.emit();
  }
}
