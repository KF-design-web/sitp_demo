import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { LANG_STORAGE_KEY } from './app/core/i18n';


document.documentElement.lang =
  (localStorage.getItem(LANG_STORAGE_KEY) ?? 'en') === 'fr' ? 'fr' : 'en';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
