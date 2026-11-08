import { Component, computed, inject, signal } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { BatchCreateToolCertificationsCommand } from '../../tool-certification.model';

export interface PersonOptionViewModel {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface CreateToolCertificationsDialogData {
  toolId: string;
  toolName: string;
  // the instructors of the tool – only they certify
  certifiers: PersonOptionViewModel[];
  // people without a certification of the tool
  candidates: PersonOptionViewModel[];
}

@Component({
  selector: 'schbar-create-tool-certifications-form',
  templateUrl: './create-tool-certifications-form.component.html',
  styleUrl: './create-tool-certifications-form.component.scss',
  imports: [
    FormDialogHeaderComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
  ],
  host: { class: 'form-dialog' },
})
export class CreateToolCertificationsFormComponent {
  protected readonly data: CreateToolCertificationsDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(
    MatDialogRef<CreateToolCertificationsFormComponent, BatchCreateToolCertificationsCommand>,
  );

  protected readonly certifiers = this.data.certifiers.map(toOption).sort(byName);

  // a single instructor is preselected
  protected readonly certifierId = signal<string | null>(
    this.data.certifiers.length === 1 ? this.data.certifiers[0].id : null,
  );
  protected readonly searchText = signal('');
  protected readonly selectedIds = signal<readonly string[]>([]);

  private readonly candidates = this.data.candidates.map(toOption).sort(byName);

  protected readonly shownCandidates = computed(() => {
    const searchText = this.searchText().trim().toLowerCase();
    return this.candidates.filter(
      (candidate) => !searchText || `${candidate.name} ${candidate.email}`.toLowerCase().includes(searchText),
    );
  });

  // in the order they were picked, shown above the list
  protected readonly selectedCandidates = computed(() =>
    this.selectedIds()
      .map((id) => this.candidates.find((candidate) => candidate.id === id))
      .filter((candidate) => candidate !== undefined),
  );

  protected readonly canSave = computed(() => !!this.certifierId() && this.selectedIds().length > 0);

  // --------------------------------------------------------------------------
  // actions

  protected selectCertifier(certifierId: string): void {
    this.certifierId.set(certifierId);
  }

  protected searchCandidates(searchText: string): void {
    this.searchText.set(searchText);
  }

  protected clearSearch(): void {
    this.searchText.set('');
  }

  protected toggleCandidate(candidateId: string): void {
    this.selectedIds.update((selectedIds) =>
      selectedIds.includes(candidateId)
        ? selectedIds.filter((id) => id !== candidateId)
        : [...selectedIds, candidateId],
    );
  }

  protected batchCreateToolCertifications(): void {
    const certifiedBy = this.certifierId();
    if (!certifiedBy || this.selectedIds().length === 0) return;

    const command: BatchCreateToolCertificationsCommand = {
      userIds: [...this.selectedIds()],
      toolId: this.data.toolId,
      certifiedBy,
    };
    this.dialogRef.close(command);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toOption(person: PersonOptionViewModel): PersonOptionViewModel & { name: string } {
  return { ...person, name: `${person.firstName} ${person.lastName}` };
}

// ----------------------------------------------------------------------------
// helper

function byName(a: { name: string }, b: { name: string }): number {
  return a.name.localeCompare(b.name, 'de');
}
