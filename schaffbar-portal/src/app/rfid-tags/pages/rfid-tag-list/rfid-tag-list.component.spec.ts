import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { RfidTag, WaitingUser } from '../../rfid-tag.model';
import { RfidTagsStore } from '../../rfid-tags.store';
import { RfidTagListComponent } from './rfid-tag-list.component';

describe('RfidTagListComponent', () => {
  const rfidTags: RfidTag[] = [
    {
      id: '04A31F9C',
      state: 'assigned',
      assignee: {
        userId: 'user-1',
        userName: 'Anna Becker',
        assignmentType: RfidTagAssignmentType.Fixed,
        assignedAt: new Date('2025-02-03T10:00:00Z'),
      },
    },
    { id: '1D208917', state: 'free', assignee: null },
    { id: '04D56B11', state: 'deactivated', assignee: null },
  ];

  let fixture: ComponentFixture<RfidTagListComponent>;
  let storeMock: {
    rfidTags: WritableSignal<RfidTag[]>;
    waitingUsers: WritableSignal<WaitingUser[]>;
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    reloadRfidTags: Mock;
    unassignRfidTag: Mock;
    deleteRfidTag: Mock;
  };
  let openConfirmAction: Mock;
  let openConfirmDelete: Mock;

  beforeEach(async () => {
    storeMock = {
      rfidTags: signal(rfidTags),
      waitingUsers: signal([]),
      isLoading: signal(false),
      hasLoadError: signal(false),
      reloadRfidTags: vi.fn(),
      unassignRfidTag: vi.fn(),
      deleteRfidTag: vi.fn(),
    };
    openConfirmAction = vi.fn();
    openConfirmDelete = vi.fn();

    await TestBed.configureTestingModule({
      imports: [RfidTagListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: RfidTagsStore, useValue: storeMock },
        { provide: ConfirmActionDialogService, useValue: { open: openConfirmAction } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDelete } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RfidTagListComponent);
    await fixture.whenStable();
  });

  afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  }

  function query(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  async function search(text: string): Promise<void> {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = text;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function openRowMenu(row: number): Promise<string[]> {
    fixture.nativeElement.querySelectorAll('tr[mat-row] schbar-row-menu button')[row].click();
    await fixture.whenStable();
    // the label only, without the icon name
    return Array.from(document.querySelectorAll<HTMLElement>('[mat-menu-item] .mat-mdc-menu-item-text')).map(
      (text) => text.textContent?.trim() ?? '',
    );
  }

  function clickMenuItem(text: string): void {
    Array.from(document.querySelectorAll<HTMLElement>('[mat-menu-item]'))
      .find((item) => item.textContent?.includes(text))
      ?.click();
  }

  it('should load the tags on init', () => {
    expect(storeMock.reloadRfidTags).toHaveBeenCalledOnce();
  });

  it('should show the IDs in groups of four with their state', () => {
    expect(texts('td.tag-id')).toEqual(['04A3 1F9C', '1D20 8917', '04D5 6B11']);
    expect(texts('td.mat-column-state .label')).toEqual([
      'rfidTags.states.assigned',
      'rfidTags.states.free',
      'rfidTags.states.deactivated',
    ]);
  });

  it('should link the person of an assigned tag and show type and date', () => {
    expect(query('.assignee-name')?.textContent?.trim()).toBe('Anna Becker');
    expect(query('.assignee-name')?.getAttribute('href')).toBe('/users/user-1');
    expect(texts('.no-assignee')).toEqual(['–', '–']);
  });

  it('should count the tags of every filter and in the subtitle', () => {
    expect(texts('schbar-filter-chips button')).toEqual([
      'rfidTags.pages.rfidTagList.filters.all 3',
      'rfidTags.pages.rfidTagList.filters.assigned 1',
      'rfidTags.pages.rfidTagList.filters.free 1',
      'rfidTags.pages.rfidTagList.filters.deactivated 1',
    ]);
  });

  it('should filter by state', async () => {
    fixture.nativeElement.querySelectorAll('schbar-filter-chips button')[2].click();
    await fixture.whenStable();

    expect(texts('td.tag-id')).toEqual(['1D20 8917']);
  });

  it('should search the ID with or without spaces and the name', async () => {
    await search('1d20 89');
    expect(texts('td.tag-id')).toEqual(['1D20 8917']);

    await search('04a31f');
    expect(texts('td.tag-id')).toEqual(['04A3 1F9C']);

    await search('becker');
    expect(texts('td.tag-id')).toEqual(['04A3 1F9C']);
  });

  it('should say so when no tag matches', async () => {
    await search('ffff');

    expect(texts('schbar-empty-state.no-results .title')).toEqual(['rfidTags.pages.rfidTagList.noResults.title']);
    expect(query('div.table-card')?.hidden).toBe(true);
  });

  it('should show the empty state without tags', async () => {
    storeMock.rfidTags.set([]);
    await fixture.whenStable();

    expect(texts('schbar-empty-state .title')).toEqual(['rfidTags.pages.rfidTagList.empty.title']);
    expect(query('schbar-filter-chips')).toBeNull();
  });

  it('should list the people waiting for a tag', async () => {
    expect(query('.waiting-card')).toBeNull();

    storeMock.waitingUsers.set([
      { userId: 'user-2', userName: 'Max Schulz', assignmentType: RfidTagAssignmentType.Temporary },
    ]);
    await fixture.whenStable();

    expect(texts('.waiting-card li')).toEqual(['Max Schulz rfidTags.pages.rfidTagList.waiting.entry']);
    expect(query('.waiting-card a')?.getAttribute('href')).toBe('/users/user-2');
  });

  describe('row menu', () => {
    it('should offer profile and unassign, but no delete for an assigned tag', async () => {
      expect(await openRowMenu(0)).toEqual([
        'rfidTags.pages.rfidTagList.actions.profile',
        'rfidTags.pages.rfidTagList.actions.unassign',
      ]);
      document.querySelector('.cdk-overlay-container')?.replaceChildren();

      expect(await openRowMenu(1)).toEqual(['rfidTags.pages.rfidTagList.actions.delete']);
    });

    it('should unassign the tag after confirmation', async () => {
      openConfirmAction.mockReturnValue(of(true));
      await openRowMenu(0);

      clickMenuItem('actions.unassign');

      expect(openConfirmAction).toHaveBeenCalledExactlyOnceWith({
        title: 'rfidTags.dialogs.unassignRfidTag.title',
        message: 'rfidTags.dialogs.unassignRfidTag.message',
        confirmLabel: 'rfidTags.dialogs.unassignRfidTag.confirm',
        entity: 'Anna Becker',
        destructive: true,
      });
      expect(storeMock.unassignRfidTag).toHaveBeenCalledExactlyOnceWith({ rfidTagId: '04A31F9C', userId: 'user-1' });
    });

    it('should delete the tag after confirmation', async () => {
      openConfirmDelete.mockReturnValue(of(true));
      await openRowMenu(1);

      clickMenuItem('actions.delete');

      expect(openConfirmDelete).toHaveBeenCalledExactlyOnceWith({
        title: 'rfidTags.dialogs.deleteRfidTag.title',
        entity: '1D208917',
        message: 'rfidTags.dialogs.deleteRfidTag.message',
        confirmLabel: 'rfidTags.dialogs.deleteRfidTag.confirm',
      });
      expect(storeMock.deleteRfidTag).toHaveBeenCalledExactlyOnceWith({ rfidTagId: '1D208917' });
    });

    it('should do nothing when a dialog is cancelled', async () => {
      openConfirmAction.mockReturnValue(of(false));
      await openRowMenu(0);

      clickMenuItem('actions.unassign');

      expect(storeMock.unassignRfidTag).not.toHaveBeenCalled();
    });
  });
});
