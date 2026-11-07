import { DatePipe } from '@angular/common';
import { Component, ElementRef, computed, inject, input, output, signal, viewChild } from '@angular/core';

import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { CertificationStatusChipComponent } from '../../../shared/components/certification-status-chip/certification-status-chip.component';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { ToolCertificationStatus } from '../../../shared/models/tool-certification-status';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { BatchCreateToolCertificationsCommand, DeleteToolCertificationCommand } from '../../tool-certification.model';

export interface ToolCertificationRowViewModel {
  customerId: string;
  toolId: string;
  // null if the user has been deleted
  userName: string | null;
  status: ToolCertificationStatus;
  certifiedAt: Date;
}

// a user to pick in the autocompletes
export interface UserOptionViewModel {
  id: string;
  firstName: string;
  lastName: string;
}

@Component({
  selector: 'schbar-tool-certifications',
  templateUrl: './tool-certifications.component.html',
  styleUrl: './tool-certifications.component.scss',
  imports: [
    DatePipe,
    MatAutocompleteModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    CertificationStatusChipComponent,
    TranslatePipe,
    OrDashPipe,
  ],
})
export class ToolCertificationsComponent {
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  readonly toolId = input.required<string>();
  readonly certifications = input.required<ToolCertificationRowViewModel[]>();
  readonly certifiableUsers = input.required<UserOptionViewModel[]>();
  readonly certifiers = input.required<UserOptionViewModel[]>();

  readonly certificationsCreated = output<BatchCreateToolCertificationsCommand>();
  readonly certificationDeleted = output<DeleteToolCertificationCommand>();

  protected readonly displayedColumns: string[] = ['userName', 'status', 'certifiedAt', 'actions'];

  protected readonly searchText = signal('');
  protected readonly selectedUsers = signal<UserOptionViewModel[]>([]);
  protected readonly selectedCertifier = signal<UserOptionViewModel | null>(null);
  protected readonly certifierSearchText = signal('');

  private readonly userSearchInput = viewChild<ElementRef>('userSearchInput');
  private readonly certifierSearchInput = viewChild<ElementRef>('certifierSearchInput');

  protected readonly filteredUsers = computed(() => {
    const search = this.searchText().toLowerCase();
    const selectedIds = new Set(this.selectedUsers().map((user) => user.id));
    return this.certifiableUsers().filter((user) => !selectedIds.has(user.id) && matches(user, search));
  });

  protected readonly filteredCertifiers = computed(() => {
    const search = this.certifierSearchText().toLowerCase();
    return this.certifiers().filter((user) => matches(user, search));
  });

  // --------------------------------------------------------------------------
  // actions

  protected selectUser(user: UserOptionViewModel): void {
    this.selectedUsers.update((users) => [...users, user]);
    this.searchText.set('');
    this.userSearchInput()?.nativeElement.blur();
  }

  protected removeSelectedUser(user: UserOptionViewModel): void {
    this.selectedUsers.update((users) => users.filter((u) => u.id !== user.id));
  }

  protected searchCertifiers(value: string): void {
    this.certifierSearchText.set(value);
    if (this.selectedCertifier()) {
      this.selectedCertifier.set(null);
    }
  }

  protected selectCertifier(user: UserOptionViewModel): void {
    this.selectedCertifier.set(user);
    this.certifierSearchText.set(fullName(user));
    this.certifierSearchInput()?.nativeElement.blur();
  }

  protected certifySelectedUsers(): void {
    const certifier = this.selectedCertifier();
    if (this.selectedUsers().length === 0 || !certifier) return;

    const command: BatchCreateToolCertificationsCommand = {
      userIds: this.selectedUsers().map((user) => user.id),
      toolId: this.toolId(),
      certifiedBy: certifier.id,
    };
    this.certificationsCreated.emit(command);

    this.selectedUsers.set([]);
    this.selectedCertifier.set(null);
    this.certifierSearchText.set('');
  }

  protected deleteCertification(certification: ToolCertificationRowViewModel): void {
    this.confirmDeleteDialog
      .open({
        title: 'tools.dialogs.deleteCertification.title',
        message: 'tools.dialogs.deleteCertification.message',
        entity: certification.userName ?? undefined,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteToolCertificationCommand = {
            userId: certification.customerId,
            toolId: certification.toolId,
          };
          this.certificationDeleted.emit(command);
        }
      });
  }
}

// ----------------------------------------------------------------------------
// helper

function fullName(user: UserOptionViewModel): string {
  return `${user.firstName} ${user.lastName}`;
}

function matches(user: UserOptionViewModel, search: string): boolean {
  return fullName(user).toLowerCase().includes(search);
}
