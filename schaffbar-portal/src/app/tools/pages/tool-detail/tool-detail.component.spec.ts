import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { Tool } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolDetailTabInfoComponent } from './tool-detail-tab-info/tool-detail-tab-info.component';
import { ToolDetailComponent } from './tool-detail.component';
import { ToolDetailStore } from './tool-detail.store';

describe('ToolDetailComponent', () => {
  const tool: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
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
  };

  let fixture: ComponentFixture<ToolDetailComponent>;
  let setToolId: Mock;
  let reloadTool: Mock;
  let reloadTools: Mock;

  beforeEach(async () => {
    setToolId = vi.fn();
    reloadTool = vi.fn();
    reloadTools = vi.fn();
    const detailsStoreMock = {
      isLoading: signal(false),
      hasLoadError: signal(false),
      tool: signal(tool),
      setToolId,
      reloadTool,
      // read by the tabs
      toolCertifications: signal([]),
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
        provideTranslateService(),
        { provide: ToolsStore, useValue: { tools: signal([tool]), reloadTools } },
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

  it('should load the tool and all tools for the checks of the tabs', () => {
    expect(setToolId).toHaveBeenCalledOnce();
    expect(reloadTools).toHaveBeenCalledOnce();
  });

  it('should show the name and the info tab', () => {
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Bandsäge');
    expect(fixture.debugElement.query(By.directive(ToolDetailTabInfoComponent))).not.toBeNull();
  });

  it('should reload the tool and all tools', () => {
    fixture.nativeElement.querySelector('.tool-detail-header button').click();

    expect(reloadTool).toHaveBeenCalledOnce();
    expect(reloadTools).toHaveBeenCalledTimes(2);
  });
});
