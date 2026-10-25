import { DatePipe, registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import localeDe from '@angular/common/locales/de';
import {
  ApplicationConfig,
  isDevMode,
  LOCALE_ID,
  provideCheckNoChangesConfig,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';

import { provideTranslateService, TranslatePipe } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';
import { provideDateAdapter } from './shared/date/provide-date-adapter';

registerLocaleData(localeDe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(),
    provideDateAdapter(),
    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: '/i18n/',
        suffix: '.json',
      }),
      fallbackLang: 'de',
      lang: 'de',
    }),
    ...(isDevMode() ? [provideCheckNoChangesConfig({ exhaustive: true, interval: 1000 })] : []),
    DatePipe,
    TranslatePipe,
    { provide: LOCALE_ID, useValue: 'de-DE' },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
  ],
};
