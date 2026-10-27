import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MatSnackBar } from '@angular/material/snack-bar';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let snackBar: { open: Mock };

  beforeEach(() => {
    snackBar = { open: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should not show a snackbar for successful requests', () => {
    http.get('/api/tools').subscribe();
    httpMock.expectOne('/api/tools').flush([]);

    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('should show "load failed" for a failed GET and pass the error on', () => {
    const errorHandler = vi.fn();
    http.get('/api/tools').subscribe({ error: errorHandler });

    httpMock.expectOne('/api/tools').flush('boom', { status: 500, statusText: 'Server Error' });

    expect(snackBar.open).toHaveBeenCalledExactlyOnceWith('shared.errors.loadFailed', 'shared.actions.close', {
      duration: 6000,
    });
    expect(errorHandler).toHaveBeenCalledOnce();
    expect(errorHandler.mock.calls[0][0]).toBeInstanceOf(HttpErrorResponse);
  });

  it('should show "save failed" for a failed mutation', () => {
    http.put('/api/tools/tool-1', {}).subscribe({ error: () => undefined });

    httpMock.expectOne('/api/tools/tool-1').flush('conflict', { status: 409, statusText: 'Conflict' });

    expect(snackBar.open).toHaveBeenCalledWith('shared.errors.saveFailed', 'shared.actions.close', expect.anything());
  });

  it('should show "server unreachable" when there is no response', () => {
    http.delete('/api/tools/tool-1').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/tools/tool-1').error(new ProgressEvent('error'), { status: 0 });

    expect(snackBar.open).toHaveBeenCalledWith(
      'shared.errors.serverUnreachable',
      'shared.actions.close',
      expect.anything(),
    );
  });
});
