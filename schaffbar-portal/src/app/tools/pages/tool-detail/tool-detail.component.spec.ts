import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { UpdateToolFormComponent } from '../../components/update-tool-form/update-tool-form.component';
import { ToolListItem, UpdateToolCommand } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolDetailTabInfoComponent } from './tool-detail-tab-info/tool-detail-tab-info.component';
import { ToolDetailComponent } from './tool-detail.component';
import { ToolDetailStore } from './tool-detail.store';

describe('ToolDetailComponent', () => {
  const tool: ToolListItem = {
    id: 'tool-1',
    name: 'Bandsäge',
    description: null,
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Red,
    rfidReaderId: null,
    wlanRelaisType: null,
    ipAddress: null,
    httpStartCommand: null,
    onCommand: null,
    offCommand: null,
    instructors: [],
    usedBy: 'Anna Becker',
  };

  let fixture: ComponentFixture<ToolDetailComponent>;
  let detailsStoreMock: Record<string, unknown> & {
    setToolId: Mock;
    reloadTool: Mock;
    updateTool: Mock;
    deleteTool: Mock;
  };
  let reloadTools: Mock;
  let openDialog: Mock;
  let openConfirmDelete: Mock;

  beforeEach(async () => {
    reloadTools = vi.fn();
    openDialog = vi.fn();
    openConfirmDelete = vi.fn();
    detailsStoreMock = {
      isLoading: signal(false),
      hasLoadError: signal(false),
      tool: signal(tool),
      setToolId: vi.fn(),
      reloadTool: vi.fn(),
      updateTool: vi.fn(),
      deleteTool: vi.fn(),
      // read by the tabs
      toolCertifications: signal(
        ['user-1', 'user-2'].map((customerId) => ({
          id: `cert-${customerId}`,
          customerId,
          toolId: 'tool-1',
          status: ToolCertificationStatus.Active,
          certifiedAt: new Date('2026-01-01T10:00:00Z'),
          certifiedBy: null,
        })),
      ),
      activeCertificationsCount: signal(2),
      assignedRfidReader: signal(null),
      users: signal([]),
      assignableRfidReaders: signal([]),
      instructors: signal([]),
      instructorCandidates: signal([]),
      certifiers: signal([]),
      certifiableUsers: signal([]),
    };

    await TestBed.configureTestingModule({
      imports: [ToolDetailComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        { provide: ToolsStore, useValue: { tools: signal([tool]), reloadTools } },
        { provide: MatDialog, useValue: { open: openDialog } },
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDelete } },
      ],
    })
      .overrideComponent(ToolDetailComponent, {
        set: { providers: [{ provide: ToolDetailStore, useValue: detailsStoreMock }] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ToolDetailComponent);
    fixture.componentRef.setInput('id', 'tool-1');
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

  it('should load the tool and all tools for the checks of the tabs', () => {
    expect(detailsStoreMock.setToolId).toHaveBeenCalledOnce();
    expect(reloadTools).toHaveBeenCalledOnce();
  });

  it('should show the name with area, requirement and the person using it, and link back to the list', () => {
    expect(texts('h1')).toEqual(['Bandsäge']);
    expect(texts('.meta .label')).toEqual([
      'shared.toolAreas.HOLZ',
      'shared.certificationRequirements.RED',
      'tools.pages.toolList.inUseBy',
    ]);
    expect(query('.breadcrumbs a')?.getAttribute('href')).toBe('/tools');
  });

  it('should name the tabs like the draft, with the number of certifications', () => {
    expect(texts('[role="tab"]')).toEqual([
      'tools.tabGroups.toolDetails.detailsTab',
      'tools.tabGroups.toolDetails.certificationsTab 2',
      'tools.tabGroups.toolDetails.configurationTab',
    ]);
  });

  it('should switch to the hardware tab when the details tab asks for it', async () => {
    fixture.debugElement
      .query(By.directive(ToolDetailTabInfoComponent))
      .componentInstance.hardwareConfigurationRequested.emit();
    await fixture.whenStable();

    expect(query('[role="tab"][aria-selected="true"]')?.textContent?.trim()).toBe(
      'tools.tabGroups.toolDetails.configurationTab',
    );
  });

  it('should edit the tool from the header', () => {
    const command: UpdateToolCommand = {
      toolId: 'tool-1',
      name: 'Bandsäge',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Yellow,
    };
    openDialog.mockReturnValue({ afterClosed: () => of(command) });

    query('schbar-page-header .actions button[matButton="outlined"]')?.click();

    expect(openDialog).toHaveBeenCalledWith(
      UpdateToolFormComponent,
      expect.objectContaining({ data: expect.objectContaining({ tool, activeCertificationsCount: 2 }) }),
    );
    expect(detailsStoreMock.updateTool).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('should delete the tool from the ⋯ menu after confirmation', async () => {
    openConfirmDelete.mockReturnValue(of(true));

    query('schbar-row-menu button')?.click();
    await fixture.whenStable();
    document.querySelector<HTMLElement>('.danger-menu-item')?.click();

    expect(openConfirmDelete).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ entity: 'Bandsäge' }));
    expect(detailsStoreMock.deleteTool).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1' });
  });

  it('should reload the tool and the list', () => {
    query('schbar-page-header button[matIconButton]')?.click();

    expect(detailsStoreMock.reloadTool).toHaveBeenCalledOnce();
    expect(reloadTools).toHaveBeenCalledTimes(2);
  });
});
