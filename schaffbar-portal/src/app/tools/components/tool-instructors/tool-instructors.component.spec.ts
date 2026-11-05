import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ToolInstructorsComponent, UserOptionViewModel } from './tool-instructors.component';

describe('ToolInstructorsComponent', () => {
  const instructors: UserOptionViewModel[] = [
    { id: 'user-1', firstName: 'Max', lastName: 'Mustermann' },
    { id: 'user-2', firstName: 'Erika', lastName: 'Musterfrau' },
  ];
  const candidates: UserOptionViewModel[] = [
    { id: 'user-3', firstName: 'Paul', lastName: 'Panzer' },
    { id: 'user-4', firstName: 'Karl', lastName: 'Klammer' },
  ];

  let fixture: ComponentFixture<ToolInstructorsComponent>;
  let loader: HarnessLoader;
  let added: Mock;
  let removed: Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolInstructorsComponent],
      providers: [provideZonelessChangeDetection(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolInstructorsComponent);
    fixture.componentRef.setInput('toolId', 'tool-1');
    fixture.componentRef.setInput('instructors', instructors);
    fixture.componentRef.setInput('candidates', candidates);
    added = vi.fn();
    removed = vi.fn();
    fixture.componentInstance.instructorsAdded.subscribe(added);
    fixture.componentInstance.instructorsRemoved.subscribe(removed);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  async function optionTexts(autocomplete: MatAutocompleteHarness): Promise<string[]> {
    const options = await autocomplete.getOptions();
    return Promise.all(options.map((option) => option.getText()));
  }

  it('should list the instructors', async () => {
    const checkboxes = await loader.getAllHarnesses(MatCheckboxHarness);

    expect(await Promise.all(checkboxes.map((checkbox) => checkbox.getLabelText()))).toEqual([
      'Max Mustermann',
      'Erika Musterfrau',
    ]);
  });

  it('should offer the candidates that are not selected yet, filtered by the search text', async () => {
    const autocomplete = await loader.getHarness(MatAutocompleteHarness);

    await autocomplete.focus();
    expect(await optionTexts(autocomplete)).toEqual(['Paul Panzer', 'Karl Klammer']);

    await autocomplete.selectOption({ text: 'Paul Panzer' });
    // after a selection the panel only opens again while typing
    await autocomplete.enterText('k');
    expect(await optionTexts(autocomplete)).toEqual(['Karl Klammer']);
  });

  it('should emit the add command with the selected candidates', async () => {
    await (await loader.getHarness(MatAutocompleteHarness)).selectOption({ text: 'Karl Klammer' });

    await (
      await loader.getHarness(MatButtonHarness.with({ text: /tools.pages.toolDetails.instructors.add$/ }))
    ).click();

    expect(added).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', instructorIds: ['user-4'] });
  });

  it('should emit the remove command with the checked instructors', async () => {
    const removeButton = await loader.getHarness(
      MatButtonHarness.with({ text: /tools.pages.toolDetails.instructors.remove$/ }),
    );
    expect(await removeButton.isDisabled()).toBe(true);

    await (await loader.getHarness(MatCheckboxHarness.with({ label: 'Erika Musterfrau' }))).check();
    await removeButton.click();

    expect(removed).toHaveBeenCalledExactlyOnceWith({ toolId: 'tool-1', instructorIds: ['user-2'] });
  });
});
