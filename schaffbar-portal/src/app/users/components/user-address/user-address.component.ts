import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, FormRoot, readonly, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { UpdateUserAddressCommand } from '../../user.model';
import { POSTAL_CODE_LENGTH, validatePostalCode } from '../../user.validators';

export interface AddressViewModel {
  addressLine1: string;
  addressLine2: string | null;
  postalCode: string;
  city: string;
  country: string;
}

export interface UserAddressViewModel {
  id: string;
  address: AddressViewModel;
}

interface AddressFormModel {
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
  readonly userAddress = input.required<UserAddressViewModel>();

  readonly addressChanged = output<UpdateUserAddressCommand>();

  protected readonly isEditing = signal(false);

  protected readonly postalCodeLength = POSTAL_CODE_LENGTH;

  // follows the userAddress input
  private readonly model = linkedSignal(() => toAddressFormModel(this.userAddress().address));

  protected readonly addressForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
    required(schema.addressLine1);
    validatePostalCode(schema.postalCode);
    required(schema.city);
    required(schema.country);
  });

  // --------------------------------------------------------------------------
  // actions

  protected editAddress(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.addressForm.addressLine1().focusBoundControl();
  }

  protected discardAddressChanges(): void {
    this.model.set(toAddressFormModel(this.userAddress().address));
    this.addressForm().reset();
    this.isEditing.set(false);
  }

  protected saveAddress(): void {
    submit(this.addressForm, async () => {
      const { addressLine1, addressLine2, postalCode, city, country } = this.model();
      const command: UpdateUserAddressCommand = {
        id: this.userAddress().id,
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

// ----------------------------------------------------------------------------
// mappers

function toAddressFormModel(address: AddressViewModel): AddressFormModel {
  return { ...address, addressLine2: address.addressLine2 ?? '' };
}
