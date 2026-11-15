import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';

import { MatSnackBar } from '@angular/material/snack-bar';

import { TranslateService } from '@ngx-translate/core';
import { catchError, throwError } from 'rxjs';

import { SNACKBAR_DURATION_MS } from '../../shared/snack-bar/snack-bar-constants';

/**
 * Shows a snackbar for every failed HTTP request and passes the error on, so stores and resources still get it.
 * A rule the backend rejects comes as a problem detail (RFC 9457) with a stable `code` – its translation
 * `shared.errors.<CODE>` explains it. Without a code, or for a code the portal does not translate yet, the message
 * depends on the kind of failure: server not reachable, loading (GET) or saving (all other methods).
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  // resolved lazily, only when an error occurs: the translation files are loaded via HttpClient (and this interceptor)
  // while TranslateService is being created – injecting it here directly coincided with translations not loading
  const injector = inject(Injector);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const snackBar = injector.get(MatSnackBar);
        const translate = injector.get(TranslateService);
        snackBar.open(toMessage(error, request.method, translate), translate.instant('shared.actions.close'), {
          duration: SNACKBAR_DURATION_MS,
        });
      }
      return throwError(() => error);
    }),
  );
};

function toMessage(error: HttpErrorResponse, method: string, translate: TranslateService): string {
  const problemKey = toProblemKey(error);
  if (problemKey) {
    const problemMessage: string = translate.instant(problemKey);
    // instant() returns the key itself when there is no translation
    if (problemMessage !== problemKey) return problemMessage;
  }
  return translate.instant(toMessageKey(error, method));
}

function toProblemKey(error: HttpErrorResponse): string | null {
  const body: unknown = error.error;
  const code = typeof body === 'object' && body !== null && 'code' in body ? body.code : null;
  return typeof code === 'string' && /^[A-Z][A-Z_]*$/.test(code) ? `shared.errors.${code}` : null;
}

function toMessageKey(error: HttpErrorResponse, method: string): string {
  if (error.status === 0) return 'shared.errors.serverUnreachable';
  return method === 'GET' ? 'shared.errors.loadFailed' : 'shared.errors.saveFailed';
}
