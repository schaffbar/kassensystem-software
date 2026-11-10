import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface UserContactCardViewModel {
  email: string;
  phone: string | null;
  dateOfBirth: Date;
  address: {
    addressLine1: string;
    addressLine2: string | null;
    postalCode: string;
    city: string;
    country: string;
  };
}

@Component({
  selector: 'schbar-user-contact-card',
  templateUrl: './user-contact-card.component.html',
  styleUrl: './user-contact-card.component.scss',
  imports: [DatePipe, MatButtonModule, MatIconModule, TranslatePipe, OrDashPipe],
  host: { class: 'card' },
})
export class UserContactCardComponent {
  readonly userContactCard = input.required<UserContactCardViewModel>();

  readonly editRequested = output<void>();
}
