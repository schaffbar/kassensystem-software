import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, FormRoot, readonly, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { UpdateUserAddressCommand, User, UserAddress } from '../../shared/models/user.model';
import { POSTAL_CODE_LENGTH, validatePostalCode } from '../../shared/validators/user.validators';

interface AddressModel {
  addressLine1: string;
  addressLine2: string;
  postalCode: string;
  city: string;
  country: string;
}

@Component({
  selector: 'schbar-user-address',
  templateUrl: './user-address.component.html',
  styleUrl: './user-address.component.scss',
  imports: [FormField, FormRoot, FieldErrorComponent, MatInputModule, MatButtonModule, MatIconModule, TranslatePipe],
})
export class UserAddressComponent {
  readonly user = input.required<User>();

  readonly addressChanged = output<UpdateUserAddressCommand>();

  protected readonly isEditing = signal(false);

  protected readonly postalCodeLength = POSTAL_CODE_LENGTH;

  // follows the user input
  private readonly model = linkedSignal(() => toModel(this.user().address));

  protected readonly addressForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
    required(schema.addressLine1);
    validatePostalCode(schema.postalCode);
    required(schema.city);
    required(schema.country);
  });

  protected onEdit(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.addressForm.addressLine1().focusBoundControl();
  }

  protected onCancel(): void {
    this.model.set(toModel(this.user().address));
    this.addressForm().reset();
    this.isEditing.set(false);
  }

  protected onSave(): void {
    submit(this.addressForm, async () => {
      const { addressLine1, addressLine2, postalCode, city, country } = this.model();
      const command: UpdateUserAddressCommand = {
        id: this.user().id,
        addressLine1,
        addressLine2: addressLine2 || undefined,
        postalCode,
        city,
        country,
      };
      this.addressChanged.emit(command);
      this.isEditing.set(false);
    });
  }
}

function toModel(address: UserAddress): AddressModel {
  return { ...address, addressLine2: address.addressLine2 ?? '' };
}
