import { Component, input, output } from '@angular/core';

import { UserAddressComponent } from '../../components/user-address/user-address.component';
import { UserContactComponent } from '../../components/user-contact/user-contact.component';
import { UpdateUserAddressCommand, UpdateUserContactCommand, User } from '../../shared/models/user.model';

@Component({
  selector: 'schbar-user-detail-info',
  templateUrl: './user-detail-info.component.html',
  styleUrl: './user-detail-info.component.scss',
  imports: [UserAddressComponent, UserContactComponent],
})
export class UserDetailInfoComponent {
  readonly user = input.required<User>();

  readonly addressChanged = output<UpdateUserAddressCommand>();
  readonly contactChanged = output<UpdateUserContactCommand>();

  protected onAddressChanged(command: UpdateUserAddressCommand) {
    this.addressChanged.emit(command);
  }

  protected onContactChanged(command: UpdateUserContactCommand) {
    this.contactChanged.emit(command);
  }
}
