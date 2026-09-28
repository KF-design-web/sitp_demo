import { ChangeDetectionStrategy, Component, input } from '@angular/core';


@Component({
  selector: 'app-error-slip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (message()) {
      <div
        role="alert"
        class="rounded-lg border-l-4 border-error bg-error-soft p-4 text-sm text-error"
      >
        {{ message() }}
      </div>
    }
  `,
})
export class ErrorSlip {
  message = input<string>('');
}
