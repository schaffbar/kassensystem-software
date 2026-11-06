import { computed, provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CloseSessionDialogService } from '../../../../shared/dialogs/close-session-dialog/close-session-dialog.service';
import { WorkshopSession } from '../../../workshop-session.model';
import { UserDetailStore } from '../user-detail.store';
import { UserDetailTabSessionComponent } from './user-detail-tab-session.component';

describe('UserDetailTabSessionComponent', () => {
  const session = {
    id: 'session-1',
    customerId: 'user-1',
    workshopUsages: [
      { id: 'usage-1', entryTime: new Date('2026-10-31T10:00:00Z'), exitTime: new Date('2026-10-31T11:00:00Z') },
    ],
    totalWorkshopDurationInMinutes: 60,
    totalWorkshopUnits: 10,
    toolUsageSummaries: [],
  } as unknown as WorkshopSession;

  let fixture: ComponentFixture<UserDetailTabSessionComponent>;
  let hasRunningUsage: WritableSignal<boolean>;
  let storeMock: Record<string, unknown> & { closeWorkshopSession: Mock; reloadOpenSession: Mock };
  let openDialog: Mock;

  beforeEach(async () => {
    const openSession = signal<WorkshopSession | null>(session);
    hasRunningUsage = signal(false);
    storeMock = {
      openSession,
      slots: computed(() => openSession()?.workshopUsages ?? []),
      toolUsageSummaries: computed(() => openSession()?.toolUsageSummaries ?? []),
      totalWorkshopDurationInMinutes: signal(60),
      totalWorkshopUnits: signal(10),
      hasRunningUsage,
      closeWorkshopSession: Object.assign(vi.fn(), { isPending: signal(false) }),
      reloadOpenSession: vi.fn(),
    };
    openDialog = vi.fn();

    await TestBed.configureTestingModule({
      imports: [UserDetailTabSessionComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: UserDetailStore, useValue: storeMock },
        { provide: CloseSessionDialogService, useValue: { open: openDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailTabSessionComponent);
    fixture.componentRef.setInput('userId', 'user-1');
    await fixture.whenStable();
  });

  function closeButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.close-button');
  }

  async function clickClose(confirmed: boolean): Promise<void> {
    openDialog.mockReturnValue(of(confirmed));
    closeButton().click();
    await fixture.whenStable();
  }

  it('should disable the button and show a hint while a usage is running', async () => {
    hasRunningUsage.set(true);
    await fixture.whenStable();

    expect(closeButton().disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('.running-usage-hint').textContent).toContain(
      'users.pages.openSession.messages.runningUsage',
    );
  });

  it('should open the dialog for the user', async () => {
    await clickClose(false);

    expect(openDialog).toHaveBeenCalledExactlyOnceWith('user-1');
  });

  it('should close the session of the user after confirmation', async () => {
    await clickClose(true);

    expect(storeMock.closeWorkshopSession).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
    expect(storeMock.reloadOpenSession).not.toHaveBeenCalled();
  });

  it('should reload the session when the dialog is cancelled, as the dialog may have shown a newer state', async () => {
    await clickClose(false);

    expect(storeMock.closeWorkshopSession).not.toHaveBeenCalled();
    expect(storeMock.reloadOpenSession).toHaveBeenCalledOnce();
  });
});
