import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { SessionService } from '../../core/session.service';
import { LangService } from '../../core/lang.service';
import { Button } from '../../ui/button/button';
import { ConfirmModal } from '../../ui/confirm-modal/confirm-modal';

@Component({
  selector: 'app-welcome-page',
  imports: [Button, RouterLink, ConfirmModal],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto mt-8 max-w-md rounded-xl border border-line bg-surface p-8 shadow-card">
      <h1 class="text-[28px] font-bold text-ink">{{ t('welcome.heading') }}</h1>

      <p class="mt-4 text-base text-ink">
        {{ t('welcome.logged-in-as') }}
        <span class="font-semibold">{{ user()?.username }}</span>
      </p>

      <p class="mt-2 text-sm text-muted">
        <a
          routerLink="/courses"
          class="font-medium text-primary underline-offset-2 transition-colors hover:text-primary-dark hover:underline focus:outline-none focus:ring-2 focus:ring-primary"
        >{{ t('welcome.browse-courses') }}</a>
      </p>

      <div class="mt-8">
        <app-button variant="secondary" [loading]="loggingOut()" (click)="askLogout()">
          {{ t('header.logout') }}
        </app-button>
      </div>
    </div>

    @if (confirmingLogout()) {
      <app-confirm-modal
        [title]="t('logout-confirm.title')"
        [text]="t('logout-confirm.text')"
        [confirmLabel]="t('logout-confirm.confirm')"
        [cancelLabel]="t('logout-confirm.cancel')"
        (confirm)="logout()"
        (cancel)="confirmingLogout.set(false)"
      />
    }
  `,
})
export class WelcomePage {
  private session = inject(SessionService);
  private authService = inject(AuthService);
  private langService = inject(LangService);
  private router = inject(Router);

  readonly loggingOut = signal(false);
  readonly confirmingLogout = signal(false);
  readonly user = this.session.user;

  constructor() {
    if (this.session.user() === null) {
      this.router.navigate(['/login']);
    }
  }

  askLogout(): void {
    this.confirmingLogout.set(true);
  }

  t(key: string): string {
    return this.langService.t(key);
  }

  logout(): void {
    this.loggingOut.set(true);
    this.authService.logout().subscribe({
      next: () => this.afterLogout(),
      error: (err: { status?: number }) => {
        if (err?.status === 401) {
          this.afterLogout();
        } else {
          this.loggingOut.set(false);
          console.warn('Logout failed: hotel unreachable.', err);
        }
      },
    });
  }

  private afterLogout(): void {
    this.session.clear();
    this.router.navigate(['/login']);
  }
}
