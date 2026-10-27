import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ActiveUser } from '../../dashboard.model';
import { ActiveUsersWidgetComponent } from './active-users-widget.component';

describe('ActiveUsersWidgetComponent', () => {
  const activeUsers: ActiveUser[] = [
    { customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann', entryTime: '2026-10-27T08:30:00' },
    { customerId: 'user-2', firstName: 'Erika', lastName: 'Musterfrau', entryTime: '2026-10-27T09:55:00' },
  ];

  let fixture: ComponentFixture<ActiveUsersWidgetComponent>;

  beforeEach(async () => {
    vi.useFakeTimers({ now: new Date(2026, 9, 27, 10, 0, 0) });

    await TestBed.configureTestingModule({
      imports: [ActiveUsersWidgetComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ActiveUsersWidgetComponent);
    fixture.componentRef.setInput('activeUsers', activeUsers);
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  function durations(): string[] {
    const cells: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('td.mat-column-duration');
    return Array.from(cells).map((cell) => cell.textContent?.trim() ?? '');
  }

  it('should show the duration since the entry', () => {
    expect(durations()).toEqual(['1h 30m', '5m']);
  });

  it('should update the duration every minute without reloading', () => {
    vi.advanceTimersByTime(60_000);
    fixture.detectChanges();

    expect(durations()).toEqual(['1h 31m', '6m']);
  });

  it('should emit the selected user when a row is clicked', () => {
    const selected = vi.fn();
    fixture.componentInstance.userSelected.subscribe(selected);

    fixture.nativeElement.querySelector('tr[mat-row]').click();

    expect(selected).toHaveBeenCalledWith(expect.objectContaining({ customerId: 'user-1' }));
  });
});
