import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MatDialog } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../../environments/environment';
import { CloseSessionDialogContainerComponent } from './close-session-dialog-container.component';
import { CloseSessionDialogService } from './close-session-dialog.service';

describe('CloseSessionDialogService', () => {
  let service: CloseSessionDialogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
      ],
    });
    service = TestBed.inject(CloseSessionDialogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    TestBed.inject(MatDialog).closeAll();
    httpMock.verify();
  });

  it('should open the dialog with the customer id as input', () => {
    service.open('user-1').subscribe();
    TestBed.tick();

    const [dialogRef] = TestBed.inject(MatDialog).openDialogs;
    expect(dialogRef.componentInstance).toBeInstanceOf(CloseSessionDialogContainerComponent);
    httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/workshop-sessions/user-1/active`).flush(null);
  });

  it('should emit true when the dialog is confirmed', async () => {
    const result = firstValueFrom(service.open('user-1'));
    TestBed.tick();
    httpMock.expectOne(() => true).flush(null);

    TestBed.inject(MatDialog).openDialogs[0].close(true);

    expect(await result).toBe(true);
  });

  it('should emit false when the dialog is closed otherwise', async () => {
    const result = firstValueFrom(service.open('user-1'));
    TestBed.tick();
    httpMock.expectOne(() => true).flush(null);

    TestBed.inject(MatDialog).openDialogs[0].close();

    expect(await result).toBe(false);
  });
});
