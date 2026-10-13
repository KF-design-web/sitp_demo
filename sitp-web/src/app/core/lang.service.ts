import { Injectable, signal } from '@angular/core';
import { contentFr, i18n, Lang, LANG_STORAGE_KEY } from './i18n';

@Injectable({ providedIn: 'root' })
export class LangService {

  private static readonly API_KEYS: Record<string, string> = {
    'Invalid or expired code.': 'error.400-code',
    'This code has already been used.': 'error.api.code-used',
    'This code was revoked.': 'error.api.code-revoked',
    'Access denied.': 'error.api.access-denied',
    'Course not found.': 'error.api.course-not-found',
    'Count must be between 1 and 500.': 'error.api.count-bounds',
    'Not authenticated.': 'error.api.not-authenticated',
    'Unexpected server error.': 'error.api.unexpected',
  };

  readonly lang = signal<Lang>(
    (localStorage.getItem(LANG_STORAGE_KEY) ?? 'en') === 'fr' ? 'fr' : 'en'
  );

  setLang(l: Lang): void {
    this.lang.set(l);
    localStorage.setItem(LANG_STORAGE_KEY, l);
    document.documentElement.lang = l;
  }

  t(key: string): string {
    const entry = i18n[key];
    if (!entry) {
      return key;
    }
    return entry[this.lang()] ?? entry['en'] ?? key;
  }

  tText(en: string): string {
    if (!en || this.lang() === 'en') {
      return en;
    }
    return contentFr[en] ?? en;
  }

  tApiError(msg: string | null | undefined): string {
    if (!msg) {
      return this.t('error.fallback');
    }
    if (msg.startsWith('Missing required parameter:')) {
      const param = msg.slice('Missing required parameter:'.length).trim().replace(/\.$/, '');
      return this.t('error.api.missing-param').replace('${param}', param);
    }
    const key = LangService.API_KEYS[msg];
    return key ? this.t(key) : msg;
  }
}
