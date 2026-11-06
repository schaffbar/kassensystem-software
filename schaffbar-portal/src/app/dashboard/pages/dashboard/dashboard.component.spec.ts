import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ActiveToolsWidgetComponent } from '../../components/active-tools-widget/active-tools-widget.component';
import { ActiveUsersWidgetComponent } from '../../components/active-users-widget/active-users-widget.component';
import { ActiveTool, ActiveUser, Tool } from '../../dashboard.model';
import { DashboardStore } from '../../dashboard.store';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  const tools: Tool[] = [
    { id: 'tool-1', name: 'Bandsäge' },
    { id: 'tool-2', name: 'Drechselbank' },
  ];
  const activeTools: ActiveTool[] = [
    { toolId: 'tool-2', toolName: 'Drechselbank', customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann' },
  ];
  const activeUsers: ActiveUser[] = [
    { customerId: 'user-1', firstName: 'Max', lastName: 'Mustermann', entryTime: new Date('2026-10-27T08:30:00') },
  ];

  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    const storeMock = {
      isLoading: signal(false),
      hasLoadError: signal(false),
      tools: signal(tools),
      activeTools: signal(activeTools),
      activeUsers: signal(activeUsers),
      reloadActiveUsers: vi.fn(),
      reloadActiveTools: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService(), provideRouter([])],
    })
      .overrideComponent(DashboardComponent, { set: { providers: [{ provide: DashboardStore, useValue: storeMock }] } })
      .compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    await fixture.whenStable();
  });

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should pass every tool with the user who is using it', () => {
    expect(child(ActiveToolsWidgetComponent).toolStatuses()).toEqual([
      { name: 'Bandsäge', usedBy: null },
      { name: 'Drechselbank', usedBy: { firstName: 'Max', lastName: 'Mustermann' } },
    ]);
  });

  it('should open the session tab of the selected user', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    child(ActiveUsersWidgetComponent).userSelected.emit('user-1');

    expect(navigate).toHaveBeenCalledExactlyOnceWith(['users', 'user-1'], { queryParams: { tab: 'open-session' } });
  });
});
