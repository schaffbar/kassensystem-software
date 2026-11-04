import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { MatDialogRef } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { environment } from '../../../../environments/environment';
import { WorkshopSessionApiModel } from '../../api/workshop-sessions/workshop-session.api-model';
import {
  toolUsageSummaryFixture,
  workshopSessionFixture,
  workshopUsageFixture,
} from '../../api/workshop-sessions/workshop-session.api-fixtures';
import { CloseSessionDialogModalComponent } from './close-session-dialog-modal.component';
import { CloseSessionDialogContainerComponent } from './close-session-dialog-container.component';

describe('CloseSessionDialogContainerComponent', () => {
  const sessionUrl = `${environment.apiBaseUrl}/api/v1/workshop-sessions/user-1/active`;

  let fixture: ComponentFixture<CloseSessionDialogContainerComponent>;
  let httpMock: HttpTestingController;
  let dialogRef: { close: Mock };

  beforeEach(async () => {
    dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CloseSessionDialogContainerComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CloseSessionDialogContainerComponent);
    fixture.componentRef.setInput('customerId', 'user-1');
    // whenStable() would wait for the pending request forever
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  function modal(): CloseSessionDialogModalComponent {
    return fixture.debugElement.query(By.directive(CloseSessionDialogModalComponent)).componentInstance;
  }

  async function respond(session: WorkshopSessionApiModel | null): Promise<void> {
    httpMock.expectOne(sessionUrl).flush(session);
    await fixture.whenStable();
  }

  it('should load the session of the customer and pass the loading state on', async () => {
    expect(modal().isLoading()).toBe(true);

    await respond(session({ running: false }));

    expect(modal().isLoading()).toBe(false);
  });

  it('should pass the session on as view model', async () => {
    await respond(session({ running: true }));

    expect(modal().viewModel()).toEqual({
      workshop: { minutes: 60, units: 10 },
      tools: [{ toolId: 'tool-1', name: 'Bandsäge', minutes: 31, units: 6 }],
      hasRunningUsage: true,
    });
  });

  it('should pass null on when there is no open session', async () => {
    await respond(null);

    expect(modal().viewModel()).toBeNull();
  });

  it('should pass a load error on', async () => {
    httpMock.expectOne(sessionUrl).flush('error', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(modal().hasLoadError()).toBe(true);
  });

  it('should close the dialog with true on confirm', async () => {
    await respond(session({ running: false }));

    modal().confirmed.emit();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(true);
  });

  it('should close the dialog with false on cancel', async () => {
    await respond(session({ running: false }));

    modal().cancelled.emit();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith(false);
  });
});

function session({ running }: { running: boolean }): WorkshopSessionApiModel {
  return workshopSessionFixture({
    workshopUsages: [workshopUsageFixture({ exitTime: running ? null : '2026-10-31T11:00:00Z' })],
    toolUsageSummaries: [toolUsageSummaryFixture()],
  });
}
