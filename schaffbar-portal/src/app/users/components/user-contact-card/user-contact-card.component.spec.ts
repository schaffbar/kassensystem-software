import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { UserContactCardComponent, UserContactCardViewModel } from './user-contact-card.component';

describe('UserContactCardComponent', () => {
  const contact: UserContactCardViewModel = {
    email: 'max@example.org',
    phone: null,
    dateOfBirth: new Date(1990, 0, 31),
    address: {
      addressLine1: 'Hauptstraße 1',
      addressLine2: 'Hinterhaus',
      postalCode: '12345',
      city: 'Berlin',
      country: 'Deutschland',
    },
  };

  let fixture: ComponentFixture<UserContactCardComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [UserContactCardComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    });
    fixture = TestBed.createComponent(UserContactCardComponent);
    fixture.componentRef.setInput('userContactCard', contact);
    await fixture.whenStable();
  });

  function text(selector: string): string {
    return fixture.nativeElement.querySelector(selector)?.textContent.replace(/\s+/g, ' ').trim();
  }

  it('should show email as link, a missing phone as dash and the full address', () => {
    expect(fixture.nativeElement.querySelector('.email a').getAttribute('href')).toBe('mailto:max@example.org');
    expect(text('.phone span')).toBe('–');
    expect(text('.address span')).toBe('Hauptstraße 1 Hinterhaus 12345 Berlin, Deutschland');
    expect(text('.date-of-birth span')).toBe('users.pages.userDetails.contact.bornOn');
  });

  it('should request editing', () => {
    const editRequested = vi.fn();
    fixture.componentInstance.editRequested.subscribe(editRequested);

    fixture.nativeElement.querySelector('.card-header button').click();

    expect(editRequested).toHaveBeenCalledTimes(1);
  });
});
