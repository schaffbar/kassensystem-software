import { Component, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { UpdateUserCommand } from '../../user.model';
import { DATE_OF_BIRTH_MAX, DATE_OF_BIRTH_MIN, validateDateOfBirth } from '../../user.validators';

export interface UpdateUserViewModel {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  clubMember: boolean;
}

export interface UpdateUserDialogData {
  user: UpdateUserViewModel;
}

interface UpdateUserFormModel {
  firstName: string;
  lastName: string;
  // null = no date selected (the datepicker has no "empty" value other than null)
  dateOfBirth: Date | null;
  clubMember: boolean;
}

@Component({
  selector: 'schbar-update-user-form',
  templateUrl: './update-user-form.component.html',
  styleUrl: './update-user-form.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatDatepickerModule,
    TranslatePipe,
  ],
})
export class UpdateUserFormComponent {
  private readonly data: UpdateUserDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UpdateUserFormComponent, UpdateUserCommand>);

  protected readonly minDate = DATE_OF_BIRTH_MIN;
  protected readonly maxDate = DATE_OF_BIRTH_MAX;

  private readonly model = signal<UpdateUserFormModel>(toModel(this.data.user));

  protected readonly userForm = form(this.model, (schema) => {
    required(schema.firstName);
    required(schema.lastName);
    validateDateOfBirth(schema.dateOfBirth);
  });

  protected onSave(): void {
    submit(this.userForm, async () => {
      const { firstName, lastName, dateOfBirth, clubMember } = this.model();
      if (!dateOfBirth) return;

      const command: UpdateUserCommand = { id: this.data.user.id, firstName, lastName, dateOfBirth, clubMember };
      this.dialogRef.close(command);
    });
  }
}

function toModel(user: UpdateUserViewModel): UpdateUserFormModel {
  return {
    firstName: user.firstName,
    lastName: user.lastName,
    dateOfBirth: user.dateOfBirth,
    clubMember: user.clubMember,
  };
}
