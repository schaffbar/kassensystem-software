import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { UserSessionBannerComponent, UserSessionBannerViewModel } from './user-session-banner.component';

describe('UserSessionBannerComponent', () => {
  const inWorkshop: UserSessionBannerViewModel = {
    startTime: new Date(2026, 10, 9, 14, 5),
    inWorkshop: true,
    runningTool: { name: 'Drechselbank', startTime: new Date(2026, 10, 9, 14, 40) },
    checkedOutAt: null,
    canClose: false,
  };

  let fixture: ComponentFixture<UserSessionBannerComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UserSessionBannerComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    });
    fixture = TestBed.createComponent(UserSessionBannerComponent);
  });

  async function render(viewModel: UserSessionBannerViewModel): Promise<void> {
    fixture.componentRef.setInput('userSessionBanner', viewModel);
    await fixture.whenStable();
  }

  function query<T extends HTMLElement = HTMLElement>(selector: string): T | null {
    return fixture.nativeElement.querySelector(selector);
  }

  it('should show the running tool and lock closing while the user is in the workshop', async () => {
    await render(inWorkshop);

    expect(query('.title')?.textContent?.trim()).toBe('users.pages.userDetails.session.inWorkshopSince');
    expect(query('.running-tool')).not.toBeNull();
    expect(query('.dot')?.classList).toContain('pulse');
    expect(query<HTMLButtonElement>('.close-button')?.disabled).toBe(true);
    expect(query('.close-button')?.getAttribute('aria-describedby')).toBe('close-session-hint');
    expect(query('#close-session-hint')?.textContent?.trim()).toBe(
      'users.pages.userDetails.session.closeAfterCheckout',
    );
  });

  it('should explain a tool still running after checking out', async () => {
    await render({ ...inWorkshop, inWorkshop: false, checkedOutAt: new Date(2026, 10, 9, 16, 30) });

    expect(query('#close-session-hint')?.textContent?.trim()).toBe(
      'users.pages.userDetails.session.closeAfterToolStopped',
    );
  });

  it('should offer closing after checking out', async () => {
    await render({
      ...inWorkshop,
      inWorkshop: false,
      runningTool: null,
      checkedOutAt: new Date(2026, 10, 9, 16, 30),
      canClose: true,
    });
    const closeRequested = vi.fn();
    fixture.componentInstance.closeRequested.subscribe(closeRequested);

    expect(query('.checked-out')?.textContent?.trim()).toBe('users.pages.userDetails.session.checkedOut');
    expect(query('.dot')?.classList).not.toContain('pulse');
    expect(query('#close-session-hint')).toBeNull();
    query<HTMLButtonElement>('.close-button')?.click();

    expect(closeRequested).toHaveBeenCalledTimes(1);
  });
});
