import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Languages } from 'lucide-angular';
import { LangService } from '../../core/lang.service';
import { SessionService } from '../../core/session.service';
import { AuthService } from '../../auth/auth.service';
import { Button } from '../button/button';

@Component({
  selector: 'app-header-bar',
  imports: [LucideAngularModule, RouterLink, Button],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="h-16 border-b border-line bg-surface">
      <div class="mx-auto flex h-full max-w-6xl items-center justify-between px-8 md:px-4">
        <div class="flex items-center gap-6">
          <a routerLink="/" class="text-xl font-bold text-ink">SITP</a>
          <a
            routerLink="/courses"
            class="text-sm font-medium text-muted transition-colors hover:text-ink focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {{ t('header.courses') }}
          </a>
        </div>

        <div class="flex items-center gap-4">
          @if (user(); as u) {
            <span class="hidden text-sm text-muted sm:inline">{{ u.email }}</span>
            <app-button variant="secondary" compact (click)="logout()">
              {{ t('header.logout') }}
            </app-button>
          } @else {
            <app-button variant="primary" compact (click)="goLogin()">
              {{ t('header.login') }}
            </app-button>
          }

          <span class="flex items-center gap-1 text-sm" aria-label="Language">
            <lucide-icon [img]="languages" [size]="16" class="text-muted" />
            <button
              type="button"
              (click)="setLang('en')"
              class="rounded px-1 py-0.5 transition-colors hover:text-ink focus:outline-none focus:ring-2 focus:ring-primary"
              [class.text-primary]="lang() === 'en'"
              [class.font-semibold]="lang() === 'en'"
              [class.text-muted]="lang() !== 'en'"
            >EN</button>
            <span class="text-muted">|</span>
            <button
              type="button" (click)="setLang('fr')"
              class="rounded px-1 py-0.5 transition-colors hover:text-ink focus:outline-none focus:ring-2 focus:ring-primary"
              [class.text-primary]="lang() === 'fr'"
              [class.font-semibold]="lang() === 'fr'"
              [class.text-muted]="lang() !== 'fr'"
            >FR</button>
          </span>
        </div>
      </div>
    </header>
  `,
})
export class HeaderBar {
  private langService = inject(LangService);
  private session = inject(SessionService);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly languages = Languages;
  readonly lang = this.langService.lang;
  readonly user = this.session.user;

  t(key: string): string {
    return this.langService.t(key);
  }

  setLang(l: 'en' | 'fr'): void {
    this.langService.setLang(l);
  }

  goLogin(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.afterLogout(),
      error: (err: { status?: number }) => {
        if (err?.status === 401) {
          this.afterLogout();
        } else {
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
