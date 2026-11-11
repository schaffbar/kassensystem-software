import { Component, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { TimeWithDayPipe } from '../../../shared/pipes/time-with-day.pipe';

export interface UserSessionBannerViewModel {
  // first entry of the session
  startTime: Date;
  inWorkshop: boolean;
  // last exit at the gate – null while in the workshop
  checkedOutAt: Date | null;
  canClose: boolean;
}

@Component({
  selector: 'schbar-user-session-banner',
  templateUrl: './user-session-banner.component.html',
  styleUrl: './user-session-banner.component.scss',
  imports: [MatButtonModule, MatIconModule, TranslatePipe, TimeWithDayPipe],
})
export class UserSessionBannerComponent {
  readonly userSessionBanner = input.required<UserSessionBannerViewModel>();

  readonly closeRequested = output<void>();
}
