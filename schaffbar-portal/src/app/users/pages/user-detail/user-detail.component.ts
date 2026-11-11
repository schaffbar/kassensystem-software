import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, linkedSignal } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE, USER_DETAIL_TAB } from '../../../app.routes';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { RfidTagAssignmentStatus } from '../../../shared/models/rfid-tag-assignment-status';
import { RfidTagIdPipe } from '../../../shared/pipes/rfid-tag-id.pipe';
import { AgeGroupKeyPipe } from '../../../shared/pipes/age-group-key.pipe';
import {
  UserSessionBannerComponent,
  UserSessionBannerViewModel,
} from '../../components/user-session-banner/user-session-banner.component';
import { RfidTagAssignmentTypeKeyPipe } from '../../rfid-tag-assignment-type-key.pipe';
import { canCloseWorkshopSession, isInWorkshop, WorkshopSession } from '../../workshop-session.model';
import { UserDetailDialogsService } from './user-detail-dialogs.service';
import { UserDetailTabCertificationsComponent } from './user-detail-tab-certifications/user-detail-tab-certifications.component';
import { UserDetailTabOverviewComponent } from './user-detail-tab-overview/user-detail-tab-overview.component';
import { UserDetailTabSessionComponent } from './user-detail-tab-session/user-detail-tab-session.component';
import { UserDetailStore } from './user-detail.store';

const SESSION_TAB = 2;

@Component({
  selector: 'schbar-user-detail',
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.scss',
  imports: [
    DatePipe,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTabsModule,
    TranslatePipe,
    PageHeaderComponent,
    RowMenuComponent,
    StatusBadgeComponent,
    AgeGroupKeyPipe,
    RfidTagIdPipe,
    RfidTagAssignmentTypeKeyPipe,
    UserSessionBannerComponent,
    UserDetailTabOverviewComponent,
    UserDetailTabCertificationsComponent,
    UserDetailTabSessionComponent,
  ],
  providers: [UserDetailStore, UserDetailDialogsService],
})
export class UserDetailComponent {
  protected readonly detailsStore = inject(UserDetailStore);
  private readonly dialogs = inject(UserDetailDialogsService);

  // id and tab come from the route parameters
  readonly id = input.required<string>();
  readonly tab = input<string>();

  protected readonly usersLink = `/${ROUTE.USERS}`;
  protected readonly RfidTagAssignmentStatus = RfidTagAssignmentStatus;

  protected readonly selectedTab = linkedSignal<number>(() =>
    this.tab() === USER_DETAIL_TAB.OPEN_SESSION ? SESSION_TAB : 0,
  );

  protected readonly userName = computed(() => {
    const user = this.detailsStore.user();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });

  protected readonly initials = computed(() => {
    const user = this.detailsStore.user();
    return user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase() : '';
  });

  protected readonly sessionBanner = computed(() => {
    const session = this.detailsStore.openSession();
    return session ? toUserSessionBannerViewModel(session) : null;
  });

  constructor() {
    this.detailsStore.setUserId(this.id);
    this.detailsStore.pollRfidTagAssignment(this.detailsStore.isWaitingForRfidTag);
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadUser(): void {
    this.detailsStore.reloadUser();
  }

  protected editUser(): void {
    this.dialogs.editUser();
  }

  protected assignRfidTag(): void {
    this.dialogs.assignRfidTag();
  }

  protected closeWorkshopSession(): void {
    this.dialogs.closeWorkshopSession();
  }

  protected deleteUser(): void {
    this.dialogs.deleteUser();
  }

  protected showOpenSession(): void {
    this.selectedTab.set(SESSION_TAB);
  }
}

// ----------------------------------------------------------------------------
// mappers

function toUserSessionBannerViewModel(session: WorkshopSession): UserSessionBannerViewModel {
  const inWorkshop = isInWorkshop(session);
  const exitTimes = session.workshopUsages.flatMap((usage) => (usage.exitTime ? [usage.exitTime.getTime()] : []));
  return {
    startTime: session.startTime,
    inWorkshop,
    checkedOutAt: !inWorkshop && exitTimes.length > 0 ? new Date(Math.max(...exitTimes)) : null,
    canClose: canCloseWorkshopSession(session),
  };
}
