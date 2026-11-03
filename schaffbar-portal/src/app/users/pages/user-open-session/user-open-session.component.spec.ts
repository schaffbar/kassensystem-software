import { computed, provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CloseSessionDialogService } from '../../../shared/dialogs/close-session-dialog/close-session-dialog.service';
import { CloseWorkshopSessionCommand, WorkshopSession } from '../../shared/models/workshop-session.model';
import { UserDetailStore } from '../../shared/stores/user-detail.store';
import { UserOpenSessionComponent } from './user-open-session.component';

describe('UserOpenSessionComponent', () => {
  const session = {
    id: 'session-1',
    customerId: 'user-1',
    workshopUsages: [{ id: 'usage-1', entryTime: '2026-10-31T10:00:00Z', exitTime: '2026-10-31T11:00:00Z' }],
    totalWorkshopDurationInMinutes: 60,
    totalWorkshopUnits: 10,
    toolUsageSummaries: [],
  } as unknown as WorkshopSession;

  let fixture: ComponentFixture<UserOpenSessionComponent>;
  let hasRunningUsage: WritableSignal<boolean>;
  let storeMock: Record<string, unknown> & { closeSession: Mock; reloadOpenSession: Mock };
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
      closeSession: Object.assign(vi.fn(), { isPending: signal(false) }),
      reloadOpenSession: vi.fn(),
    };
    openDialog = vi.fn();

    await TestBed.configureTestingModule({
      imports: [UserOpenSessionComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: UserDetailStore, useValue: storeMock },
        { provide: CloseSessionDialogService, useValue: { open: openDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserOpenSessionComponent);
    fixture.componentRef.setInput('userId', 'user-1');
    await fixture.whenStable();
  });

  function closeButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.close-button');
  }

  async function clickClose(dialogResult: CloseWorkshopSessionCommand | undefined): Promise<void> {
    openDialog.mockReturnValue(of(dialogResult));
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
    await clickClose(undefined);

    expect(openDialog).toHaveBeenCalledExactlyOnceWith('user-1');
  });

  it('should close the session after confirmation', async () => {
    await clickClose({ customerId: 'user-1' });

    expect(storeMock.closeSession).toHaveBeenCalledExactlyOnceWith({ customerId: 'user-1' });
    expect(storeMock.reloadOpenSession).not.toHaveBeenCalled();
  });

  it('should reload the session when the dialog is cancelled, as the dialog may have shown a newer state', async () => {
    await clickClose(undefined);

    expect(storeMock.closeSession).not.toHaveBeenCalled();
    expect(storeMock.reloadOpenSession).toHaveBeenCalledOnce();
  });
});
