import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { AddToolInstructorsCommand } from '../../tool.model';

export interface InstructorCandidateViewModel {
  id: string;
  firstName: string;
  lastName: string;
  // the own certification of the tool
  certification: { status: ToolCertificationStatus; certifiedAt: Date } | null;
}

export interface AddToolInstructorsDialogData {
  toolId: string;
  toolName: string;
  candidates: InstructorCandidateViewModel[];
}

@Component({
  selector: 'schbar-add-tool-instructors-form',
  templateUrl: './add-tool-instructors-form.component.html',
  styleUrl: './add-tool-instructors-form.component.scss',
  imports: [
    DatePipe,
    FormDialogHeaderComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
  ],
  host: { class: 'form-dialog' },
})
export class AddToolInstructorsFormComponent {
  protected readonly data: AddToolInstructorsDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<AddToolInstructorsFormComponent, AddToolInstructorsCommand>);

  protected readonly searchText = signal('');
  // instructors are usually certified themselves – the filter can be switched off
  protected readonly onlyCertified = signal(true);
  protected readonly selectedIds = signal<ReadonlySet<string>>(new Set());

  protected readonly shownCandidates = computed(() => {
    const searchText = this.searchText().trim().toLowerCase();
    return this.data.candidates
      .filter((candidate) => !this.onlyCertified() || isCertified(candidate))
      .filter((candidate) => !searchText || toFullName(candidate).toLowerCase().includes(searchText))
      .map((candidate) => ({
        ...candidate,
        name: toFullName(candidate),
        initials: toInitials(candidate),
        certificationKey: toCertificationKey(candidate),
      }))
      .sort((a, b) => a.name.localeCompare(b.name, 'de'));
  });

  // --------------------------------------------------------------------------
  // actions

  protected searchCandidates(searchText: string): void {
    this.searchText.set(searchText);
  }

  protected clearSearch(): void {
    this.searchText.set('');
  }

  protected showOnlyCertified(onlyCertified: boolean): void {
    this.onlyCertified.set(onlyCertified);
  }

  protected toggleCandidate(candidateId: string): void {
    this.selectedIds.update((selectedIds) => {
      const result = new Set(selectedIds);
      if (result.has(candidateId)) {
        result.delete(candidateId);
      } else {
        result.add(candidateId);
      }
      return result;
    });
  }

  protected addToolInstructors(): void {
    const instructorIds = [...this.selectedIds()];
    if (instructorIds.length === 0) return;

    const command: AddToolInstructorsCommand = { toolId: this.data.toolId, instructorIds };
    this.dialogRef.close(command);
  }
}

// ----------------------------------------------------------------------------
// helper

function isCertified(candidate: InstructorCandidateViewModel): boolean {
  return candidate.certification?.status === ToolCertificationStatus.Active;
}

function toCertificationKey(candidate: InstructorCandidateViewModel): string {
  switch (candidate.certification?.status) {
    case ToolCertificationStatus.Active:
      return 'tools.dialogs.addInstructors.certifiedSince';
    case ToolCertificationStatus.Paused:
      return 'tools.dialogs.addInstructors.paused';
    case ToolCertificationStatus.Revoked:
      return 'tools.dialogs.addInstructors.revoked';
    default:
      return 'tools.dialogs.addInstructors.notCertified';
  }
}

function toFullName(user: { firstName: string; lastName: string }): string {
  return `${user.firstName} ${user.lastName}`;
}

function toInitials(user: { firstName: string; lastName: string }): string {
  return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
}
