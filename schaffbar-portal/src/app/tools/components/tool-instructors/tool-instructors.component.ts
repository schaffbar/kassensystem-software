import { Component, computed, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';

import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';

import { TranslatePipe } from '@ngx-translate/core';

import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { AddToolInstructorsCommand, RemoveToolInstructorsCommand } from '../../tool.model';

// an instructor or a user who can become one
export interface UserOptionViewModel {
  id: string;
  firstName: string;
  lastName: string;
}

@Component({
  selector: 'schbar-tool-instructors',
  templateUrl: './tool-instructors.component.html',
  styleUrl: './tool-instructors.component.scss',
  imports: [
    MatAutocompleteModule,
    MatButtonModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    TranslatePipe,
  ],
})
export class ToolInstructorsComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly toolId = input.required<string>();
  readonly instructors = input.required<UserOptionViewModel[]>();
  readonly candidates = input.required<UserOptionViewModel[]>();

  readonly instructorsAdded = output<AddToolInstructorsCommand>();
  readonly instructorsRemoved = output<RemoveToolInstructorsCommand>();

  protected readonly searchText = signal('');
  protected readonly selectedUsersToAdd = signal<UserOptionViewModel[]>([]);
  protected readonly selectedInstructorIdsToRemove = signal<Set<string>>(new Set());

  private readonly searchInput = viewChild<ElementRef>('searchInput');

  protected readonly filteredUsers = computed(() => {
    const search = this.searchText().toLowerCase();
    const selectedIds = this.selectedUsersToAdd().map((u) => u.id);

    return this.candidates()
      .filter((user) => !selectedIds.includes(user.id))
      .filter(
        (user) =>
          !search || user.firstName.toLowerCase().includes(search) || user.lastName.toLowerCase().includes(search),
      );
  });

  // --------------------------------------------------------------------------
  // actions

  protected selectUser(user: UserOptionViewModel): void {
    this.selectedUsersToAdd.update((users) => [...users, user]);
    this.searchText.set('');
    const input = this.searchInput();
    if (input) {
      input.nativeElement.value = '';
    }
  }

  protected removeSelectedUser(user: UserOptionViewModel): void {
    this.selectedUsersToAdd.update((users) => users.filter((u) => u.id !== user.id));
  }

  protected addInstructors(): void {
    const ids = this.selectedUsersToAdd().map((u) => u.id);
    if (ids.length > 0) {
      const command: AddToolInstructorsCommand = { toolId: this.toolId(), instructorIds: ids };
      this.instructorsAdded.emit(command);
      this.selectedUsersToAdd.set([]);
    }
  }

  protected toggleInstructorSelection(instructorId: string): void {
    this.selectedInstructorIdsToRemove.update((set) => {
      const newSet = new Set(set);
      if (newSet.has(instructorId)) {
        newSet.delete(instructorId);
      } else {
        newSet.add(instructorId);
      }
      return newSet;
    });
  }

  protected removeInstructors(): void {
    const ids = Array.from(this.selectedInstructorIdsToRemove());
    if (ids.length === 0) return;

    const names = this.instructors()
      .filter((instructor) => ids.includes(instructor.id))
      .map((instructor) => `${instructor.firstName} ${instructor.lastName}`)
      .join(', ');
    this.confirmActionDialog
      .open({
        title: 'tools.dialogs.removeInstructors.title',
        message: 'tools.dialogs.removeInstructors.message',
        entity: names,
        confirmLabel: 'tools.dialogs.removeInstructors.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: RemoveToolInstructorsCommand = { toolId: this.toolId(), instructorIds: ids };
          this.instructorsRemoved.emit(command);
          this.selectedInstructorIdsToRemove.set(new Set());
        }
      });
  }
}
