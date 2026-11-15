import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MatSnackBar } from '@angular/material/snack-bar';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
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

  describe('with a problem detail from the backend', () => {
    beforeEach(() => {
      const translate = TestBed.inject(TranslateService);
      translate.setTranslation('de', {
        shared: {
          errors: {
            RFID_READER_HAS_TOOL_ASSIGNED: 'Das Lesegerät ist einem Werkzeug zugewiesen.',
            RESOURCE_NOT_FOUND: 'Der Eintrag existiert nicht (mehr).',
          },
        },
      });
      translate.use('de');
    });

    it('should show the translation of the code', () => {
      http.delete('/api/rfid-readers/reader-1').subscribe({ error: () => undefined });

      httpMock
        .expectOne('/api/rfid-readers/reader-1')
        .flush(
          { status: 409, detail: 'Cannot delete RFID reader …', code: 'RFID_READER_HAS_TOOL_ASSIGNED' },
          { status: 409, statusText: 'Conflict' },
        );

      expect(snackBar.open).toHaveBeenCalledWith(
        'Das Lesegerät ist einem Werkzeug zugewiesen.',
        'shared.actions.close',
        expect.anything(),
      );
    });

    it('should show "not found" for a missing entry when loading', () => {
      http.get('/api/rfid-readers/reader-1').subscribe({ error: () => undefined });

      httpMock
        .expectOne('/api/rfid-readers/reader-1')
        .flush({ status: 404, code: 'RESOURCE_NOT_FOUND' }, { status: 404, statusText: 'Not Found' });

      expect(snackBar.open).toHaveBeenCalledWith(
        'Der Eintrag existiert nicht (mehr).',
        'shared.actions.close',
        expect.anything(),
      );
    });

    it('should fall back to "save failed" for a code the portal does not translate yet', () => {
      http.put('/api/tools/tool-1', {}).subscribe({ error: () => undefined });

      httpMock
        .expectOne('/api/tools/tool-1')
        .flush({ status: 409, code: 'TOOL_NAME_ALREADY_USED' }, { status: 409, statusText: 'Conflict' });

      expect(snackBar.open).toHaveBeenCalledWith('shared.errors.saveFailed', 'shared.actions.close', expect.anything());
    });

    it('should ignore a code that is not a problem code', () => {
      http.put('/api/tools/tool-1', {}).subscribe({ error: () => undefined });

      httpMock
        .expectOne('/api/tools/tool-1')
        .flush({ status: 409, code: 'actions.close' }, { status: 409, statusText: 'Conflict' });

      expect(snackBar.open).toHaveBeenCalledWith('shared.errors.saveFailed', 'shared.actions.close', expect.anything());
    });
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
