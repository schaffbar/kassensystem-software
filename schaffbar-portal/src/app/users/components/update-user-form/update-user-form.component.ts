import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';
import { isSameDay } from 'date-fns';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { UpdateUserCommand } from '../../user.model';
import { dateOfBirthRange, POSTAL_CODE_LENGTH, validateDateOfBirth, validatePostalCode } from '../../user.validators';

export interface UpdateUserViewModel {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  clubMember: boolean;
  email: string;
  phone: string | null;
  address: {
    addressLine1: string;
    addressLine2: string | null;
    postalCode: string;
    city: string;
    country: string;
  };
  createdAt: Date;
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
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  postalCode: string;
  city: string;
  country: string;
}

@Component({
  selector: 'schbar-update-user-form',
  templateUrl: './update-user-form.component.html',
  styleUrl: './update-user-form.component.scss',
  imports: [
    DatePipe,
    FormField,
    FormRoot,
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
export class UpdateUserFormComponent {
  private readonly data: UpdateUserDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UpdateUserFormComponent, UpdateUserCommand>);

  protected readonly user = this.data.user;
  // computed when the dialog opens
  protected readonly dateOfBirthLimits = dateOfBirthRange();
  protected readonly postalCodeLength = POSTAL_CODE_LENGTH;

  private readonly initialModel = toUpdateUserFormModel(this.user);
  private readonly model = signal<UpdateUserFormModel>(this.initialModel);

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

  // number of fields that differ from the saved user – nothing to save without a change
  protected readonly changedFieldsCount = computed(() => {
    const model = this.model();
    return FIELDS.filter((field) => !isSameValue(model[field], this.initialModel[field])).length;
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

  // --------------------------------------------------------------------------
  // actions

  protected saveUser(): void {
    if (this.changedFieldsCount() === 0) return;

    this.saveAttempted.set(true);
    submit(this.userForm, async () => {
      const { dateOfBirth, phone, addressLine2, ...rest } = this.model();
      if (!dateOfBirth) return;

      const command: UpdateUserCommand = {
        userId: this.user.id,
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
// mappers

function toUpdateUserFormModel(user: UpdateUserViewModel): UpdateUserFormModel {
  return {
    firstName: user.firstName,
    lastName: user.lastName,
    dateOfBirth: user.dateOfBirth,
    clubMember: user.clubMember,
    email: user.email,
    phone: user.phone ?? '',
    addressLine1: user.address.addressLine1,
    addressLine2: user.address.addressLine2 ?? '',
    postalCode: user.address.postalCode,
    city: user.address.city,
    country: user.address.country,
  };
}

// ----------------------------------------------------------------------------
// helper

const FIELDS: (keyof UpdateUserFormModel)[] = [
  'firstName',
  'lastName',
  'dateOfBirth',
  'clubMember',
  'email',
  'phone',
  'addressLine1',
  'addressLine2',
  'postalCode',
  'city',
  'country',
];

function isSameValue(
  a: UpdateUserFormModel[keyof UpdateUserFormModel],
  b: UpdateUserFormModel[keyof UpdateUserFormModel],
): boolean {
  if (a instanceof Date && b instanceof Date) return isSameDay(a, b);
  return a === b;
}
