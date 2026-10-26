import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { CertificationRequirement, Tool, ToolArea } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolDetailInfoComponent } from './tool-detail-info.component';

describe('ToolDetailInfoComponent', () => {
  const tool: Tool = {
    id: 'tool-1',
    name: 'Bandsäge',
    description: 'Große Säge',
    area: ToolArea.Holz,
    certificationRequirement: CertificationRequirement.Yellow,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
  const otherTool: Tool = { ...tool, id: 'tool-2', name: 'Drechselbank' };

  let fixture: ComponentFixture<ToolDetailInfoComponent>;
  let loader: HarnessLoader;
  let toolUpdated: Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolDetailInfoComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ToolsStore, useValue: { entities: signal([tool, otherTool]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDetailInfoComponent);
    fixture.componentRef.setInput('tool', tool);
    toolUpdated = vi.fn();
    fixture.componentInstance.toolUpdated.subscribe(toolUpdated);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function input(id: string): Promise<MatInputHarness> {
    return loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
  }

  function select(id: string): Promise<MatSelectHarness> {
    return loader.getHarness(MatSelectHarness.with({ selector: `#${id}` }));
  }

  async function click(selector: string): Promise<void> {
    const button = await loader.getHarness(MatButtonHarness.with({ selector }));
    await button.click();
    await fixture.whenStable();
  }

  const editButton = '[aria-label="Edit tool"]';
  const cancelButton = '.form-actions button[type="button"]';
  const saveButton = '.form-actions button[type="submit"]';

  it('should show the tool values read-only', async () => {
    expect(await (await input('name')).getValue()).toBe('Bandsäge');
    expect(await (await input('name')).isReadonly()).toBe(true);
    expect(await (await input('description')).isReadonly()).toBe(true);
    expect(await (await input('area')).getValue()).toBe('tools.areas.HOLZ');
    expect(await loader.getHarnessOrNull(MatSelectHarness)).toBeNull();
    expect(await loader.getHarnessOrNull(MatButtonHarness.with({ selector: saveButton }))).toBeNull();
  });

  it('should make all fields editable and focus the name after clicking edit', async () => {
    await click(editButton);

    expect(await (await input('name')).isReadonly()).toBe(false);
    expect(await (await input('name')).isFocused()).toBe(true);
    expect(await (await input('description')).isReadonly()).toBe(false);
    expect(await (await select('area')).getValueText()).toBe('tools.areas.HOLZ');
    expect(await (await select('certificationRequirement')).getValueText()).toBe(
      'tools.certificationRequirements.YELLOW',
    );
  });

  it('should emit the changes on save and switch back to read-only', async () => {
    await click(editButton);
    await (await input('name')).setValue('Kreissäge');
    const area = await select('area');
    await area.open();
    await area.clickOptions({ text: 'tools.areas.METALL' });

    await click(saveButton);

    expect(toolUpdated).toHaveBeenCalledExactlyOnceWith({
      id: 'tool-1',
      name: 'Kreissäge',
      description: 'Große Säge',
      area: ToolArea.Metall,
      certificationRequirement: CertificationRequirement.Yellow,
    });
    expect(await (await input('name')).isReadonly()).toBe(true);
  });

  it('should require a name', async () => {
    await click(editButton);
    await (await input('name')).setValue('');

    await click(saveButton);

    expect(await nameErrors()).toEqual(['shared.errors.required']);
    expect(toolUpdated).not.toHaveBeenCalled();
  });

  async function nameErrors(): Promise<string[]> {
    const nameField = await loader.getHarness(
      MatFormFieldHarness.with({ floatingLabelText: 'tools.shared.toolProperties.name' }),
    );
    return nameField.getTextErrors();
  }

  it('should reject the name of another tool (case-insensitive)', async () => {
    await click(editButton);
    await (await input('name')).setValue('DRECHSELBANK');

    await click(saveButton);

    expect(await nameErrors()).toEqual(['tools.dialogs.addTool.errors.nameExists']);
    expect(toolUpdated).not.toHaveBeenCalled();
  });

  it('should accept its own unchanged name', async () => {
    await click(editButton);

    await click(saveButton);

    expect(await nameErrors()).toEqual([]);
    expect(toolUpdated).toHaveBeenCalledOnce();
  });

  it('should restore the values on cancel without emitting', async () => {
    await click(editButton);
    await (await input('name')).setValue('Kreissäge');

    await click(cancelButton);

    expect(toolUpdated).not.toHaveBeenCalled();
    expect(await (await input('name')).getValue()).toBe('Bandsäge');
    expect(await (await input('name')).isReadonly()).toBe(true);
  });

  it('should take over the values when the tool input changes', async () => {
    fixture.componentRef.setInput('tool', { ...tool, name: 'Tischkreissäge' });
    await fixture.whenStable();

    expect(await (await input('name')).getValue()).toBe('Tischkreissäge');
  });
});
