import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';

export interface UserSessionBannerViewModel {
  // first entry of the session
  startTime: Date;
  inWorkshop: boolean;
  // null if no tool is running; name null if the tool has been deleted
  runningTool: { name: string | null; startTime: Date } | null;
  // last exit at the gate – null while in the workshop
  checkedOutAt: Date | null;
  canClose: boolean;
}

@Component({
  selector: 'schbar-user-session-banner',
  templateUrl: './user-session-banner.component.html',
  styleUrl: './user-session-banner.component.scss',
  imports: [DatePipe, MatButtonModule, MatIconModule, TranslatePipe, OrDashPipe],
})
export class UserSessionBannerComponent {
  readonly userSessionBanner = input.required<UserSessionBannerViewModel>();

  readonly closeRequested = output<void>();
}
