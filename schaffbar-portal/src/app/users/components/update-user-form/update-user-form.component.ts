import { Component, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';
import { parseISO } from 'date-fns';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { UpdateUserCommand, User } from '../../shared/models/user.model';
import { DATE_OF_BIRTH_MAX, DATE_OF_BIRTH_MIN, validateDateOfBirth } from '../../shared/validators/user.validators';

export interface UpdateUserDialogData {
  user: User;
}

interface UpdateUserModel {
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

  private readonly model = signal<UpdateUserModel>(toModel(this.data.user));

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

function toModel(user: User): UpdateUserModel {
  return {
    firstName: user.firstName,
    lastName: user.lastName,
    // the backend sends the date as ISO string ("1990-01-31"), although the model declares a Date;
    // parseISO creates a local date (new Date("1990-01-31") would be midnight UTC)
    dateOfBirth: parseISO(user.dateOfBirth),
    clubMember: user.clubMember,
  };
}
