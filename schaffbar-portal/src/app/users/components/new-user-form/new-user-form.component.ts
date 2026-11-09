import { Component, computed, inject, Signal, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required, submit } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { CreateUserCommand } from '../../user.model';
import { dateOfBirthRange, POSTAL_CODE_LENGTH, validateDateOfBirth, validatePostalCode } from '../../user.validators';

// all users – for the hint on a possible duplicate
export interface ExistingUserViewModel {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface NewUserDialogData {
  // a signal, so the hint follows the user list while the dialog is open
  existingUsers: Signal<ExistingUserViewModel[]>;
}

interface NewUserFormModel {
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
    RouterLink,
    FieldErrorComponent,
    FormDialogHeaderComponent,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatDialogModule,
    MatDatepickerModule,
    MatIconModule,
    TranslatePipe,
  ],
  host: { class: 'form-dialog' },
})
export class NewUserFormComponent {
  private readonly data: NewUserDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<NewUserFormComponent, CreateUserCommand>);

  // computed when the dialog opens
  protected readonly dateOfBirthLimits = dateOfBirthRange();
  protected readonly postalCodeLength = POSTAL_CODE_LENGTH;
  protected readonly usersLink = `/${ROUTE.USERS}`;

  private readonly model = signal<NewUserFormModel>({
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

  // set by a failed save – from then on the dialog says how many fields still need attention
  private readonly saveAttempted = signal(false);

  protected readonly invalidFieldsCount = computed(() => {
    if (!this.saveAttempted()) return 0;
    const form = this.userForm;
    const fields = [
      form.firstName,
      form.lastName,
      form.dateOfBirth,
      form.email,
      form.addressLine1,
      form.postalCode,
      form.city,
      form.country,
    ];
    return fields.filter((field) => field().invalid()).length;
  });

  // only a hint – two people may well have the same name
  protected readonly possibleDuplicate = computed(() => {
    const { firstName, lastName, email } = this.model();
    const name = toComparable(`${firstName} ${lastName}`);
    const mail = toComparable(email);
    return (
      this.data
        .existingUsers()
        .find(
          (user) =>
            (!!firstName && !!lastName && toComparable(`${user.firstName} ${user.lastName}`) === name) ||
            (!!mail && toComparable(user.email) === mail),
        ) ?? null
    );
  });

  // --------------------------------------------------------------------------
  // actions

  protected saveUser(): void {
    this.saveAttempted.set(true);
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

// ----------------------------------------------------------------------------
// helper

function toComparable(text: string): string {
  return text.trim().toLowerCase().replace(/\s+/g, ' ');
}
