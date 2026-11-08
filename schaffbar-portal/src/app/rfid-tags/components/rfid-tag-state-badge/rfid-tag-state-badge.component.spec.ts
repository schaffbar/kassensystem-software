import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { RfidTagStateBadgeComponent } from './rfid-tag-state-badge.component';

describe('RfidTagStateBadgeComponent', () => {
  let fixture: ComponentFixture<RfidTagStateBadgeComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RfidTagStateBadgeComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    });
    fixture = TestBed.createComponent(RfidTagStateBadgeComponent);
  });

  async function render(state: 'assigned' | 'free' | 'deactivated'): Promise<HTMLElement> {
    fixture.componentRef.setInput('state', state);
    await fixture.whenStable();
    return fixture.nativeElement.querySelector('schbar-status-badge');
  }

  it('should show an assigned tag as success with its text', async () => {
    const badge = await render('assigned');

    expect(badge.classList).toContain('tone-success');
    expect(badge.querySelector('mat-icon')?.textContent).toBe('check_circle');
    expect(badge.querySelector('.label')?.textContent?.trim()).toBe('rfidTags.states.assigned');
  });

  it('should show a free tag as info and a deactivated one as neutral', async () => {
    expect((await render('free')).classList).toContain('tone-info');
    expect((await render('deactivated')).classList).toContain('tone-neutral');
  });
});
