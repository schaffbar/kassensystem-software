import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { NewToolFormComponent } from '../../components/new-tool-form/new-tool-form.component';
import { CreateToolCommand, ToolListItem } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolListComponent } from './tool-list.component';

describe('ToolListComponent', () => {
  function createTool(id: string, name: string, overrides: Partial<ToolListItem> = {}): ToolListItem {
    return {
      id,
      name,
      description: null,
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Yellow,
      rfidReaderId: null,
      wlanRelaisType: null,
      ipAddress: null,
      httpStartCommand: null,
      onCommand: null,
      offCommand: null,
      instructors: [],
      usedBy: null,
      ...overrides,
    };
  }

  const tools: ToolListItem[] = [
    createTool('tool-1', 'Kreissäge'),
    createTool('tool-2', 'Drechselbank', {
      rfidReaderId: 'reader-1',
      wlanRelaisType: WlanRelaisType.ShellyPro,
      certificationRequirement: CertificationRequirement.Red,
      usedBy: 'Anna Becker',
    }),
    createTool('tool-3', 'Lasercutter', { area: ToolArea.FabLab, rfidReaderId: 'reader-2' }),
  ];

  let fixture: ComponentFixture<ToolListComponent>;
  let storeMock: {
    tools: WritableSignal<ToolListItem[]>;
    isLoading: WritableSignal<boolean>;
    hasLoadError: WritableSignal<boolean>;
    reloadTools: Mock;
    createTool: Mock & { isPending: WritableSignal<boolean> };
    deleteTool: Mock;
  };
  let openDialog: Mock;
  let openConfirmDeleteDialog: Mock;

  beforeEach(async () => {
    storeMock = {
      tools: signal(tools),
      isLoading: signal(false),
      hasLoadError: signal(false),
      reloadTools: vi.fn(),
      createTool: Object.assign(vi.fn(), { isPending: signal(false) }),
      deleteTool: vi.fn(),
    };
    openDialog = vi.fn();
    openConfirmDeleteDialog = vi.fn();

    await TestBed.configureTestingModule({
      imports: [ToolListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: ToolsStore, useValue: storeMock },
        { provide: MatDialog, useValue: { open: openDialog } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDeleteDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolListComponent);
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

  async function clickSegment(control: 'area-filter' | 'view-switch', index: number): Promise<void> {
    fixture.nativeElement.querySelectorAll(`.${control} button`)[index].click();
    await fixture.whenStable();
  }

  async function search(text: string): Promise<void> {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="search"]');
    input.value = text;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('should load the tools on init', () => {
    expect(storeMock.reloadTools).toHaveBeenCalledOnce();
  });

  it('should count the tools in use and those not set up in the subtitle', async () => {
    expect(texts('schbar-page-header .subtitle')).toEqual([
      'tools.pages.toolList.subtitle · tools.pages.toolList.notSetUp',
    ]);

    storeMock.tools.set([tools[1]]);
    await fixture.whenStable();

    expect(texts('schbar-page-header .subtitle')).toEqual(['tools.pages.toolList.subtitleOne']);
  });

  describe('cards', () => {
    it('should group the tools by area, sorted by name', () => {
      expect(texts('.section-title')).toEqual(['tools.areas.HOLZ · 2', 'tools.areas.FABLAB · 1']);
      expect(texts('.card-name')).toEqual(['Drechselbank', 'Kreissäge', 'Lasercutter']);
    });

    it('should show who uses a tool and whether it is free', () => {
      expect(texts('.card-head .label')).toEqual([
        'tools.pages.toolList.states.inUse',
        'tools.pages.toolList.states.free',
        'tools.pages.toolList.states.free',
      ]);
      // only the tool in use explains its state
      expect(texts('.card-usage')).toEqual(['tools.pages.toolList.usedBy']);
    });

    it('should show the switching hardware or what is left to set up', () => {
      expect(texts('.card-footer')).toEqual([
        'electrical_services tools.pages.toolList.hardware.setUp',
        'warning tools.pages.toolList.hardware.notSetUp',
        'warning tools.pages.toolList.hardware.notSetUp',
      ]);
      expect(fixture.nativeElement.querySelectorAll('.card-footer .todo-icon')).toHaveLength(2);
    });

    it('should link a card to the detail page of its tool', () => {
      expect(query('a.tool-card')?.getAttribute('href')).toBe('/tool-2');
    });
  });

  describe('list', () => {
    beforeEach(() => clickSegment('view-switch', 1));

    it('should show the tools in a table, sorted by name', () => {
      expect(query('.tool-cards')).toBeNull();
      expect(texts('td .tool-name')).toEqual(['Drechselbank', 'Kreissäge', 'Lasercutter']);
      expect(texts('td.mat-column-state .label')).toEqual([
        'tools.pages.toolList.inUseBy',
        'tools.pages.toolList.states.free',
        'tools.pages.toolList.states.free',
      ]);
    });

    it('should show the same hardware state as the cards', () => {
      expect(texts('td.mat-column-hardware')).toEqual([
        'electrical_services tools.pages.toolList.hardware.setUp',
        'warning tools.pages.toolList.hardware.notSetUp',
        'warning tools.pages.toolList.hardware.notSetUp',
      ]);
    });

    it('should open the detail page of a clicked tool', () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

      query('tr[mat-row]')?.click();

      expect(navigate).toHaveBeenCalledExactlyOnceWith(['tools', 'tool-2']);
    });

    describe('delete', () => {
      async function clickDelete(confirmed: boolean): Promise<void> {
        openConfirmDeleteDialog.mockReturnValue(of(confirmed));
        query('tr[mat-row] schbar-row-menu button')?.click();
        await fixture.whenStable();
        document.querySelector<HTMLElement>('.danger-menu-item')?.click();
      }

      it('should delete the tool after confirmation without opening its detail page', async () => {
        const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

        await clickDelete(true);

        expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
          title: 'tools.dialogs.deleteTool.title',
          entity: 'Drechselbank',
          message: 'tools.dialogs.deleteTool.message',
          confirmLabel: 'tools.dialogs.deleteTool.confirm',
        });
        expect(storeMock.deleteTool).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-2' });
        expect(navigate).not.toHaveBeenCalled();
      });

      it('should not delete the tool when the dialog is cancelled', async () => {
        await clickDelete(false);

        expect(storeMock.deleteTool).not.toHaveBeenCalled();
      });
    });
  });

  describe('filter and search', () => {
    it('should count the tools per area, ignoring the search', async () => {
      await search('dre');

      expect(texts('.area-filter button')).toEqual([
        'tools.pages.toolList.areaFilter.all 3',
        'tools.areas.HOLZ 2',
        'tools.areas.METALL 0',
        'tools.areas.KREATIV 0',
        'tools.areas.FABLAB 1',
      ]);
    });

    it('should show only the tools of the selected area', async () => {
      await clickSegment('area-filter', 4);

      expect(texts('.card-name')).toEqual(['Lasercutter']);
    });

    it('should show only the tools whose hardware is not set up yet', async () => {
      expect(texts('.not-set-up-filter button')).toEqual(['warning tools.pages.toolList.hardware.notSetUp 2']);

      query('.not-set-up-filter button')?.click();
      await fixture.whenStable();

      expect(texts('.card-name')).toEqual(['Kreissäge', 'Lasercutter']);
    });

    it('should leave out the hardware filter when everything is set up', async () => {
      storeMock.tools.set([tools[1]]);
      await fixture.whenStable();

      expect(query('.not-set-up-filter')).toBeNull();
    });

    it('should search by name', async () => {
      await search('LASER');

      expect(texts('.card-name')).toEqual(['Lasercutter']);
    });

    it('should say so when nothing matches', async () => {
      await clickSegment('area-filter', 2);

      expect(texts('schbar-empty-state.no-results .title')).toEqual(['tools.pages.toolList.noResults.title']);
      expect(query('.tool-cards')).toBeNull();
    });
  });

  describe('empty list', () => {
    it('should offer to add the first tool', async () => {
      storeMock.tools.set([]);
      await fixture.whenStable();

      expect(texts('schbar-empty-state .title')).toEqual(['tools.pages.toolList.empty.title']);
      expect(query('.toolbar')).toBeNull();
      expect(query('schbar-empty-state button')).not.toBeNull();
    });
  });

  describe('create', () => {
    const command: CreateToolCommand = {
      name: 'Hobelbank',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Red,
    };

    function clickAdd(): void {
      query('schbar-page-header button[matButton="filled"]')?.click();
    }

    it('should open the form with all tools for the name check and create the tool', () => {
      openDialog.mockReturnValue({ afterClosed: () => of(command) });

      clickAdd();

      expect(openDialog).toHaveBeenCalledExactlyOnceWith(
        NewToolFormComponent,
        expect.objectContaining({ ...FORM_DIALOG_CONFIG, data: { existingTools: storeMock.tools } }),
      );
      expect(storeMock.createTool).toHaveBeenCalledExactlyOnceWith(command);
    });

    it('should not create a tool when the form is cancelled', () => {
      openDialog.mockReturnValue({ afterClosed: () => of(undefined) });

      clickAdd();

      expect(storeMock.createTool).not.toHaveBeenCalled();
    });

    it('should disable adding while a tool is being created', async () => {
      storeMock.createTool.isPending.set(true);
      await fixture.whenStable();

      expect((query('schbar-page-header button[matButton="filled"]') as HTMLButtonElement).disabled).toBe(true);
    });
  });
});
