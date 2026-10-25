import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, FormRoot, maxLength, minLength, readonly, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { UserAddress } from '../../shared/models/user.model';

const POSTAL_CODE_LENGTH = 5;

@Component({
  selector: 'schbar-user-address',
  templateUrl: './user-address.component.html',
  styleUrl: './user-address.component.scss',
  imports: [FormField, FormRoot, FieldErrorComponent, MatInputModule, MatButtonModule, MatIconModule, TranslatePipe],
})
export class UserAddressComponent {
  readonly userAddress = input.required<UserAddress>();

  readonly addressChanged = output<UserAddress>();

  protected readonly isEditing = signal(false);

  protected readonly postalCodeLength = POSTAL_CODE_LENGTH;

  // follows the userAddress input
  private readonly model = linkedSignal(() => toModel(this.userAddress()));

  protected readonly addressForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
    required(schema.addressLine1);
    required(schema.postalCode);
    minLength(schema.postalCode, POSTAL_CODE_LENGTH);
    maxLength(schema.postalCode, POSTAL_CODE_LENGTH);
    required(schema.city);
    required(schema.country);
  });

  protected onEdit(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.addressForm.addressLine1().focusBoundControl();
  }

  protected onCancel(): void {
    this.model.set(toModel(this.userAddress()));
    this.addressForm().reset();
    this.isEditing.set(false);
  }

  protected onSave(): void {
    submit(this.addressForm, async () => {
      const { addressLine1, addressLine2, postalCode, city, country } = this.model();
      const address: UserAddress = { addressLine1, addressLine2, postalCode, city, country };
      this.addressChanged.emit(address);
      this.isEditing.set(false);
    });
  }
}

function toModel(address: UserAddress): UserAddress {
  return { ...address, addressLine2: address.addressLine2 ?? '' };
}
