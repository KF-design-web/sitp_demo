import { Injectable, signal } from '@angular/core';
import { contentFr, i18n, Lang, LANG_STORAGE_KEY } from './i18n';

@Injectable({ providedIn: 'root' })
export class LangService {
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
}
