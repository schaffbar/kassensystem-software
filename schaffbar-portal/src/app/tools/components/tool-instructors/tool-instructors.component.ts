import { Component, computed, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { ConfirmActionDialogService } from '../../../shared/dialogs/confirm-action-dialog/confirm-action-dialog.service';
import { RemoveToolInstructorsCommand } from '../../tool.model';

export interface ToolInstructorViewModel {
  id: string;
  firstName: string;
  lastName: string;
  certificationsGiven: number;
}

@Component({
  selector: 'schbar-tool-instructors',
  templateUrl: './tool-instructors.component.html',
  styleUrl: './tool-instructors.component.scss',
  imports: [RouterLink, MatButtonModule, MatIconModule, TranslatePipe],
  host: { class: 'card' },
})
export class ToolInstructorsComponent {
  private readonly confirmActionDialog = inject(ConfirmActionDialogService);

  readonly toolId = input.required<string>();
  readonly instructors = input.required<ToolInstructorViewModel[]>();

  readonly addRequested = output<void>();
  readonly instructorsRemoved = output<RemoveToolInstructorsCommand>();

  protected readonly usersLink = `/${ROUTE.USERS}`;

  protected readonly rows = computed(() =>
    this.instructors().map((instructor) => ({
      ...instructor,
      name: toFullName(instructor),
      initials: toInitials(instructor),
    })),
  );

  // --------------------------------------------------------------------------
  // actions

  protected removeInstructor(instructor: ToolInstructorViewModel): void {
    const isLast = this.instructors().length === 1;
    this.confirmActionDialog
      .open({
        title: 'tools.dialogs.removeInstructors.title',
        message: isLast ? 'tools.dialogs.removeInstructors.messageLast' : 'tools.dialogs.removeInstructors.message',
        entity: toFullName(instructor),
        confirmLabel: 'tools.dialogs.removeInstructors.confirm',
        destructive: true,
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: RemoveToolInstructorsCommand = { toolId: this.toolId(), instructorIds: [instructor.id] };
          this.instructorsRemoved.emit(command);
        }
      });
  }
}

// ----------------------------------------------------------------------------
// helper

function toFullName(user: { firstName: string; lastName: string }): string {
  return `${user.firstName} ${user.lastName}`;
}

function toInitials(user: { firstName: string; lastName: string }): string {
  return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
}
