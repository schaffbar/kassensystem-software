import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement } from '../../../../shared/models/certification-requirement';
import { ToolArea } from '../../../../shared/models/tool-area';
import { ToolDetailInfoComponent } from '../../../components/tool-detail-info/tool-detail-info.component';
import { ToolInstructorsComponent } from '../../../components/tool-instructors/tool-instructors.component';
import { Tool, UpdateToolCommand } from '../../../tool.model';
import { ToolsStore } from '../../../tools.store';
import { User } from '../../../user.model';
import { ToolDetailStore } from '../tool-detail.store';
import { ToolDetailTabInfoComponent } from './tool-detail-tab-info.component';

describe('ToolDetailTabInfoComponent', () => {
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
    instructors: ['user-1'],
  };
  const otherTool: Tool = { ...tool, id: 'tool-2', name: 'Drechselbank', instructors: [] };
  const instructors: User[] = [{ id: 'user-1', firstName: 'Max', lastName: 'Mustermann' }];
  const candidates: User[] = [{ id: 'user-2', firstName: 'Erika', lastName: 'Musterfrau' }];

  let fixture: ComponentFixture<ToolDetailTabInfoComponent>;
  let updateTool: Mock;
  let addToolInstructors: Mock;

  beforeEach(async () => {
    updateTool = vi.fn();
    addToolInstructors = vi.fn();
    const detailsStoreMock = {
      tool: signal(tool),
      instructors: signal(instructors),
      instructorCandidates: signal(candidates),
      updateTool,
      addToolInstructors,
    };

    await TestBed.configureTestingModule({
      imports: [ToolDetailTabInfoComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ToolDetailStore, useValue: detailsStoreMock },
        { provide: ToolsStore, useValue: { tools: signal([tool, otherTool]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDetailTabInfoComponent);
    await fixture.whenStable();
  });

  function child<T>(component: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(component)).componentInstance;
  }

  it('should pass the tool and all tools to the info', () => {
    const info = child(ToolDetailInfoComponent);

    expect(info.toolDetailInfo()).toBe(tool);
    expect(info.existingTools()).toEqual([tool, otherTool]);
  });

  it('should execute the update command of the info', () => {
    const command: UpdateToolCommand = {
      toolId: 'tool-1',
      name: 'Bandsäge',
      description: 'Große Säge',
      area: ToolArea.Holz,
      certificationRequirement: CertificationRequirement.Yellow,
    };

    child(ToolDetailInfoComponent).toolUpdated.emit(command);

    expect(updateTool).toHaveBeenCalledExactlyOnceWith(command);
  });

  it('should pass instructors and candidates and execute the add command', () => {
    const instructorsComponent = child(ToolInstructorsComponent);
    expect(instructorsComponent.instructors()).toEqual(instructors);
    expect(instructorsComponent.candidates()).toEqual(candidates);

    instructorsComponent.instructorsAdded.emit({ toolId: 'tool-1', instructorIds: ['user-2'] });

    expect(addToolInstructors).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', instructorIds: ['user-2'] });
  });
});
