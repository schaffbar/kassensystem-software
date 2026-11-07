import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';
import { MatSortHeaderHarness } from '@angular/material/sort/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { WlanRelaisType } from '../../../shared/models/wlan-relais-type';
import { NewToolFormComponent } from '../../components/new-tool-form/new-tool-form.component';
import { CreateToolCommand, Tool } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolListComponent } from './tool-list.component';

describe('ToolListComponent', () => {
  function createTool(id: string, name: string, overrides: Partial<Tool> = {}): Tool {
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
      ...overrides,
    };
  }

  const tools: Tool[] = [
    createTool('tool-1', 'Bandsäge'),
    createTool('tool-2', 'Drechselbank', { rfidReaderId: 'reader-1', wlanRelaisType: WlanRelaisType.Shelly1 }),
    createTool('tool-3', 'Kreissäge'),
  ];

  let fixture: ComponentFixture<ToolListComponent>;
  let loader: HarnessLoader;
  let storeMock: {
    tools: WritableSignal<Tool[]>;
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
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function texts(selector: string): string[] {
    const elements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll(selector);
    return Array.from(elements).map((element) => element.textContent?.trim() ?? '');
  }

  it('should load the tools on init', () => {
    expect(storeMock.reloadTools).toHaveBeenCalledOnce();
  });

  it('should show a check for an assigned RFID reader and a dash for missing values', () => {
    expect(texts('td.mat-column-rfidReaderId')).toEqual(['–', 'check', '–']);
    expect(texts('td.mat-column-wlanRelaisType')).toEqual(['–', 'tools.wlanRelaisTypes.SHELLY_1', '–']);
  });

  it('should sort by the assigned RFID reader', async () => {
    const header = await loader.getHarness(
      MatSortHeaderHarness.with({ label: 'tools.shared.toolProperties.rfidReader' }),
    );

    await header.click();
    await header.click();

    expect(texts('td.mat-column-name')[0]).toBe('Drechselbank');
  });

  it('should open the detail page of a clicked tool', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.nativeElement.querySelector('tr[mat-row]').click();

    expect(navigate).toHaveBeenCalledExactlyOnceWith(['tools', 'tool-1']);
  });

  describe('create', () => {
    const command: CreateToolCommand = {
      name: 'Standbohrmaschine',
      area: ToolArea.Metall,
      certificationRequirement: CertificationRequirement.Red,
    };

    function clickAdd(result: CreateToolCommand | undefined): void {
      openDialog.mockReturnValue({ afterClosed: () => of(result) });
      fixture.nativeElement.querySelector('.tool-list-header__actions button').click();
    }

    it('should open the form with all tools for the name check and create the tool', () => {
      clickAdd(command);

      expect(openDialog).toHaveBeenCalledExactlyOnceWith(
        NewToolFormComponent,
        expect.objectContaining({ data: { existingTools: storeMock.tools }, disableClose: true }),
      );
      expect(storeMock.createTool).toHaveBeenCalledExactlyOnceWith(command);
    });

    it('should disable adding while a tool is being created', async () => {
      storeMock.createTool.isPending.set(true);
      await fixture.whenStable();

      expect(fixture.nativeElement.querySelector('.tool-list-header__actions button').disabled).toBe(true);
    });

    it('should not create a tool when the form is cancelled', () => {
      clickAdd(undefined);

      expect(storeMock.createTool).not.toHaveBeenCalled();
    });
  });

  describe('delete', () => {
    function clickDeleteOfSecondRow(confirmed: boolean): void {
      openConfirmDeleteDialog.mockReturnValue(of(confirmed));
      fixture.nativeElement.querySelectorAll('tr[mat-row] .danger-button')[1].click();
    }

    it('should delete the tool after confirmation without opening its detail page', () => {
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');

      clickDeleteOfSecondRow(true);

      expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
        title: 'tools.dialogs.deleteTool.title',
        entity: 'Drechselbank',
        message: 'tools.dialogs.deleteTool.message',
        confirmLabel: 'tools.dialogs.deleteTool.confirm',
      });
      expect(storeMock.deleteTool).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-2' });
      expect(navigate).not.toHaveBeenCalled();
    });

    it('should not delete the tool when the dialog is cancelled', () => {
      clickDeleteOfSecondRow(false);

      expect(storeMock.deleteTool).not.toHaveBeenCalled();
    });
  });
});
