import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ToolInstructorsComponent, ToolInstructorViewModel } from './tool-instructors.component';

describe('ToolInstructorsComponent', () => {
  const instructors: ToolInstructorViewModel[] = [
    { id: 'user-1', firstName: 'Lena', lastName: 'Hoffmann', certificationsGiven: 4 },
    { id: 'user-2', firstName: 'Paul', lastName: 'Richter', certificationsGiven: 1 },
  ];

  let fixture: ComponentFixture<ToolInstructorsComponent>;
  let openConfirmAction: Mock;
  let instructorsRemoved: Mock;

  beforeEach(async () => {
    openConfirmAction = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ToolInstructorsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: ConfirmActionDialogService, useValue: { open: openConfirmAction } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolInstructorsComponent);
    fixture.componentRef.setInput('toolId', 'tool-1');
    fixture.componentRef.setInput('instructors', instructors);
    instructorsRemoved = vi.fn();
    fixture.componentInstance.instructorsRemoved.subscribe(instructorsRemoved);
    await fixture.whenStable();
  });

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  }

  function removeButtons(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.remove-button'));
  }

  it('should list the instructors with initials, link and the certifications they gave', () => {
    expect(texts('.avatar')).toEqual(['LH', 'PR']);
    expect(texts('.instructor-name')).toEqual(['Lena Hoffmann', 'Paul Richter']);
    expect(fixture.nativeElement.querySelector('.instructor-name').getAttribute('href')).toBe('/users/user-1');
    expect(texts('.instructor-meta')).toEqual([
      'tools.pages.toolDetails.certificationsGiven',
      'tools.pages.toolDetails.certificationsGivenOne',
    ]);
    expect(removeButtons()[0].getAttribute('aria-label')).toBe('tools.pages.toolDetails.actions.removeInstructorLabel');
  });

  it('should say so when there are no instructors', async () => {
    fixture.componentRef.setInput('instructors', []);
    await fixture.whenStable();

    expect(texts('.no-instructors')).toEqual(['tools.pages.toolDetails.noInstructors']);
  });

  it('should request adding instructors', () => {
    const addRequested = vi.fn();
    fixture.componentInstance.addRequested.subscribe(addRequested);

    fixture.nativeElement.querySelector('.card-header button').click();

    expect(addRequested).toHaveBeenCalledOnce();
  });

  it('should remove an instructor after confirmation', () => {
    openConfirmAction.mockReturnValue(of(true));

    removeButtons()[1].click();

    expect(openConfirmAction).toHaveBeenCalledExactlyOnceWith({
      title: 'tools.dialogs.removeInstructors.title',
      message: 'tools.dialogs.removeInstructors.message',
      entity: 'Paul Richter',
      confirmLabel: 'tools.dialogs.removeInstructors.confirm',
      destructive: true,
    });
    expect(instructorsRemoved).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', instructorIds: ['user-2'] });
  });

  it('should warn when the last instructor is removed', async () => {
    openConfirmAction.mockReturnValue(of(true));
    fixture.componentRef.setInput('instructors', [instructors[0]]);
    await fixture.whenStable();

    removeButtons()[0].click();

    expect(openConfirmAction).toHaveBeenCalledWith(
      expect.objectContaining({ message: 'tools.dialogs.removeInstructors.messageLast' }),
    );
  });

  it('should not remove an instructor when the confirmation is cancelled', () => {
    openConfirmAction.mockReturnValue(of(false));

    removeButtons()[0].click();

    expect(instructorsRemoved).not.toHaveBeenCalled();
  });
});
