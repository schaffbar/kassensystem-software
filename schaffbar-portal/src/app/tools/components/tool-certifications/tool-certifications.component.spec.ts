import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import {
  ToolCertificationRowViewModel,
  ToolCertificationsComponent,
  UserOptionViewModel,
} from './tool-certifications.component';

describe('ToolCertificationsComponent', () => {
  const certifications: ToolCertificationRowViewModel[] = [
    {
      customerId: 'user-1',
      toolId: 'tool-1',
      userName: 'Max Mustermann',
      status: ToolCertificationStatus.Active,
      certifiedAt: new Date('2026-01-01T10:00:00Z'),
    },
    {
      customerId: 'user-9',
      toolId: 'tool-1',
      userName: null,
      status: ToolCertificationStatus.Paused,
      certifiedAt: new Date('2026-02-01T10:00:00Z'),
    },
  ];
  const certifiableUsers: UserOptionViewModel[] = [
    { id: 'user-3', firstName: 'Paul', lastName: 'Panzer' },
    { id: 'user-4', firstName: 'Petra', lastName: 'Pan' },
  ];
  const certifiers: UserOptionViewModel[] = [
    { id: 'user-2', firstName: 'Erika', lastName: 'Musterfrau' },
    { id: 'user-5', firstName: 'Karl', lastName: 'Klammer' },
  ];

  let fixture: ComponentFixture<ToolCertificationsComponent>;
  let loader: HarnessLoader;
  let certificationsCreated: Mock;
  let certificationDeleted: Mock;
  let openConfirmDeleteDialog: Mock;

  beforeEach(async () => {
    openConfirmDeleteDialog = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ToolCertificationsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        { provide: ConfirmDeleteDialogService, useValue: { open: openConfirmDeleteDialog } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolCertificationsComponent);
    fixture.componentRef.setInput('toolId', 'tool-1');
    fixture.componentRef.setInput('certifications', certifications);
    fixture.componentRef.setInput('certifiableUsers', certifiableUsers);
    fixture.componentRef.setInput('certifiers', certifiers);
    certificationsCreated = vi.fn();
    certificationDeleted = vi.fn();
    fixture.componentInstance.certificationsCreated.subscribe(certificationsCreated);
    fixture.componentInstance.certificationDeleted.subscribe(certificationDeleted);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  function certifierAutocomplete(): Promise<MatAutocompleteHarness> {
    return loader.getHarness(MatAutocompleteHarness.with({ selector: '.certifier-search-field input' }));
  }

  function userAutocomplete(): Promise<MatAutocompleteHarness> {
    return loader.getHarness(MatAutocompleteHarness.with({ selector: '.user-search-field input' }));
  }

  async function optionTexts(autocomplete: MatAutocompleteHarness): Promise<string[]> {
    const options = await autocomplete.getOptions();
    return Promise.all(options.map((option) => option.getText()));
  }

  function submitButton(): Promise<MatButtonHarness> {
    return loader.getHarness(
      MatButtonHarness.with({ text: 'tools.pages.toolDetails.certifications.batchCreate.submit' }),
    );
  }

  it('should show the user name of each certification and a dash for a deleted user', () => {
    const cells: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('td.mat-column-userName');

    expect(Array.from(cells).map((cell) => cell.textContent?.trim())).toEqual(['Max Mustermann', '–']);
  });

  it('should offer only the certifiers, filtered by the search text', async () => {
    const autocomplete = await certifierAutocomplete();

    await autocomplete.focus();
    expect(await optionTexts(autocomplete)).toEqual(['Erika Musterfrau', 'Karl Klammer']);

    await autocomplete.enterText('kar');
    expect(await optionTexts(autocomplete)).toEqual(['Karl Klammer']);
  });

  it('should offer only the certifiable users that are not selected yet', async () => {
    const autocomplete = await userAutocomplete();

    await autocomplete.focus();
    expect(await optionTexts(autocomplete)).toEqual(['Paul Panzer', 'Petra Pan']);

    await autocomplete.selectOption({ text: 'Paul Panzer' });
    // after a selection the panel only opens again while typing
    await autocomplete.enterText('p');
    expect(await optionTexts(autocomplete)).toEqual(['Petra Pan']);
  });

  it('should enable submit only with a certifier and at least one user', async () => {
    expect(await (await submitButton()).isDisabled()).toBe(true);

    await (await userAutocomplete()).selectOption({ text: 'Paul Panzer' });
    expect(await (await submitButton()).isDisabled()).toBe(true);

    await (await certifierAutocomplete()).selectOption({ text: 'Erika Musterfrau' });
    expect(await (await submitButton()).isDisabled()).toBe(false);
  });

  it('should emit the batch create command and reset the selection', async () => {
    const users = await userAutocomplete();
    await users.selectOption({ text: 'Paul Panzer' });
    await users.enterText('Petra');
    await users.selectOption({ text: 'Petra Pan' });
    await (await certifierAutocomplete()).selectOption({ text: 'Erika Musterfrau' });

    await (await submitButton()).click();

    expect(certificationsCreated).toHaveBeenCalledExactlyOnceWith({
      userIds: ['user-3', 'user-4'],
      toolId: 'tool-1',
      certifiedBy: 'user-2',
    });
    expect(await (await submitButton()).isDisabled()).toBe(true);
    expect(await (await certifierAutocomplete()).getValue()).toBe('');
  });

  async function clickDelete(): Promise<void> {
    const buttons = await loader.getAllHarnesses(MatButtonHarness.with({ selector: '.danger-button' }));
    await buttons[0].click();
  }

  it('should emit the delete command after confirmation', async () => {
    openConfirmDeleteDialog.mockReturnValue(of(true));

    await clickDelete();

    expect(openConfirmDeleteDialog).toHaveBeenCalledExactlyOnceWith({
      title: 'users.dialogs.certifications.deleteCertification.title',
      message: 'users.dialogs.certifications.deleteCertification.message',
      entity: 'Max Mustermann',
    });
    expect(certificationDeleted).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1', toolId: 'tool-1' });
  });

  it('should not emit the delete command when cancelled', async () => {
    openConfirmDeleteDialog.mockReturnValue(of(false));

    await clickDelete();

    expect(certificationDeleted).not.toHaveBeenCalled();
  });
});
