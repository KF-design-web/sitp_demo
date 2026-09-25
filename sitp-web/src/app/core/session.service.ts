import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { UserResponse } from '../auth/auth.models';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private authService = inject(AuthService);

  readonly user = signal<UserResponse | null>(null);

  setUser(u: UserResponse): void {
    this.user.set(u);
  }

  clear(): void {
    this.user.set(null);
  }

  restore(): Promise<'authenticated' | 'logged-out' | 'offline'> {
    if (this.user() !== null) {
      return Promise.resolve('authenticated');
    }
    return new Promise((resolve) => {
      this.authService.me().subscribe({
        next: (me) => {
          this.setUser(me);
          resolve('authenticated');
        },
        error: (err: { status?: number }) => {
          this.clear();
          if (err?.status === 401) {
            resolve('logged-out');
          } else {
            console.warn('Boot question: hotel unreachable — starting logged-out.', err);
            resolve('offline');
          }
        },
      });
    });
  }
}
