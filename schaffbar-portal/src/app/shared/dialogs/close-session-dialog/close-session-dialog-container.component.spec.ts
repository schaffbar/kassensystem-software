import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { MatDialogRef } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { environment } from '../../../../environments/environment';
import {
  toolUsageSummaryFixture,
  workshopSessionFixture,
  workshopUsageFixture,
} from '../../api/workshop-sessions/workshop-session.api-fixtures';
import { WorkshopSessionApiModel } from '../../api/workshop-sessions/workshop-session.api-model';
import { CloseSessionDialogContainerComponent } from './close-session-dialog-container.component';
import { CloseSessionDialogModalComponent } from './close-session-dialog-modal.component';

describe('CloseSessionDialogContainerComponent', () => {
  const sessionUrl = `${environment.apiBaseUrl}/api/v1/workshop-sessions/user-1/open`;

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
    fixture.componentRef.setInput('userName', 'Max Mustermann');
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

  it('should pass the name and the session with its times on as view model', async () => {
    await respond(session({ running: false }));

    expect(modal().userName()).toBe('Max Mustermann');
    expect(modal().viewModel()).toEqual({
      startTime: new Date('2026-10-31T10:00:00Z'),
      workshop: {
        startTime: new Date('2026-10-31T10:00:00Z'),
        endTime: new Date('2026-10-31T11:00:00Z'),
        minutes: 60,
        units: 10,
      },
      tools: [
        {
          toolId: 'tool-1',
          name: 'Bandsäge',
          startTime: new Date('2026-10-31T10:05:00Z'),
          endTime: new Date('2026-10-31T10:36:00Z'),
          minutes: 31,
          units: 6,
        },
      ],
      spansSeveralDays: false,
      hasRunningUsage: false,
    });
  });

  // the times are a whole day apart or close to noon UTC – the result is the same in every time zone of the test run
  describe('times on several days', () => {
    const sameDay = (time: string) => `2026-11-10T${time}:00Z`;
    const nextDay = (time: string) => `2026-11-11T${time}:00Z`;

    async function spansSeveralDays(data: {
      entryTime?: string;
      exitTime?: string | null;
      tools?: { startTime: string; endTime: string | null }[];
    }): Promise<boolean | undefined> {
      await respond(
        workshopSessionFixture({
          startTime: data.entryTime ?? sameDay('10:00'),
          workshopUsages: [
            workshopUsageFixture({
              entryTime: data.entryTime ?? sameDay('10:00'),
              exitTime: data.exitTime === undefined ? sameDay('14:00') : data.exitTime,
            }),
          ],
          toolUsageSummaries: (data.tools ?? [{ startTime: sameDay('10:30'), endTime: sameDay('11:00') }]).map(
            (tool, index) =>
              toolUsageSummaryFixture({
                toolId: `tool-${index + 1}`,
                running: tool.endTime === null,
                usages: [
                  {
                    id: `tool-usage-${index + 1}`,
                    startTime: tool.startTime,
                    endTime: tool.endTime,
                    durationInMinutes: 30,
                    unitsUsed: 5,
                  },
                ],
              }),
          ),
        }),
      );
      return modal().viewModel()?.spansSeveralDays;
    }

    it('should not when stay and tools are on one day', async () => {
      expect(await spansSeveralDays({})).toBe(false);
    });

    it('should not when there are no tools and the stay is on one day', async () => {
      expect(await spansSeveralDays({ tools: [] })).toBe(false);
    });

    it('should when the stay ends on another day than it began', async () => {
      expect(await spansSeveralDays({ exitTime: nextDay('10:00'), tools: [] })).toBe(true);
    });

    it('should when the stay is on one day and a tool ends on another day', async () => {
      expect(
        await spansSeveralDays({
          exitTime: sameDay('14:00'),
          tools: [
            { startTime: sameDay('10:30'), endTime: sameDay('11:00') },
            { startTime: sameDay('11:00'), endTime: nextDay('11:00') },
          ],
        }),
      ).toBe(true);
    });

    it('should when a tool is used on another day than the stay', async () => {
      expect(await spansSeveralDays({ tools: [{ startTime: nextDay('10:30'), endTime: nextDay('11:00') }] })).toBe(
        true,
      );
    });

    it('should not count the end of a running stay or tool', async () => {
      expect(await spansSeveralDays({ exitTime: null, tools: [{ startTime: sameDay('10:30'), endTime: null }] })).toBe(
        false,
      );
    });

    it('should when a running stay began on another day than a tool was used', async () => {
      expect(await spansSeveralDays({ exitTime: null, tools: [{ startTime: nextDay('10:30'), endTime: null }] })).toBe(
        true,
      );
    });
  });

  it('should pass a running stay on without end time', async () => {
    await respond(session({ running: true }));

    expect(modal().viewModel()?.workshop.endTime).toBeNull();
    expect(modal().viewModel()?.hasRunningUsage).toBe(true);
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
