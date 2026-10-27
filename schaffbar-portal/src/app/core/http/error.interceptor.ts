import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';

import { MatSnackBar } from '@angular/material/snack-bar';

import { TranslateService } from '@ngx-translate/core';
import { catchError, throwError } from 'rxjs';

const SNACKBAR_DURATION_MS = 6000;

/**
 * Shows a snackbar for every failed HTTP request and passes the error on, so stores and resources still get it.
 * The backend does not send user-facing messages (no exception mapping), so the message only depends on the kind of
 * failure: server not reachable, loading (GET) or saving (all other methods).
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
        snackBar.open(
          translate.instant(toMessageKey(error, request.method)),
          translate.instant('shared.actions.close'),
          { duration: SNACKBAR_DURATION_MS },
        );
      }
      return throwError(() => error);
    }),
  );
};

function toMessageKey(error: HttpErrorResponse, method: string): string {
  if (error.status === 0) return 'shared.errors.serverUnreachable';
  return method === 'GET' ? 'shared.errors.loadFailed' : 'shared.errors.saveFailed';
}
