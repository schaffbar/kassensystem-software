import { Component, inject, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { CreateUserCommand } from '../../shared/models/user.model';
import {
  DATE_OF_BIRTH_MAX,
  DATE_OF_BIRTH_MIN,
  POSTAL_CODE_LENGTH,
  validateDateOfBirth,
  validatePostalCode,
} from '../../shared/validators/user.validators';

interface NewUserModel {
  firstName: string;
  lastName: string;
  // null = no date selected (the datepicker has no "empty" value other than null)
  dateOfBirth: Date | null;
  clubMember: boolean;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  postalCode: string;
  city: string;
  country: string;
}

@Component({
  selector: 'schbar-new-user-form',
  templateUrl: './new-user-form.component.html',
  styleUrl: './new-user-form.component.scss',
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
export class NewUserFormComponent {
  private readonly dialogRef = inject(MatDialogRef<NewUserFormComponent, CreateUserCommand>);

  protected readonly minDate = DATE_OF_BIRTH_MIN;
  protected readonly maxDate = DATE_OF_BIRTH_MAX;
  protected readonly postalCodeLength = POSTAL_CODE_LENGTH;

  private readonly model = signal<NewUserModel>({
    firstName: '',
    lastName: '',
    dateOfBirth: null,
    clubMember: false,
    email: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    postalCode: '',
    city: '',
    country: 'Deutschland',
  });

  protected readonly userForm = form(this.model, (schema) => {
    required(schema.firstName);
    required(schema.lastName);
    validateDateOfBirth(schema.dateOfBirth);
    required(schema.email);
    email(schema.email);
    required(schema.addressLine1);
    validatePostalCode(schema.postalCode);
    required(schema.city);
    required(schema.country);
  });

  protected onSave(): void {
    submit(this.userForm, async () => {
      const { dateOfBirth, phone, addressLine2, ...rest } = this.model();
      if (!dateOfBirth) return;

      const command: CreateUserCommand = {
        ...rest,
        dateOfBirth,
        phone: phone || undefined,
        addressLine2: addressLine2 || undefined,
      };
      this.dialogRef.close(command);
    });
  }
}
