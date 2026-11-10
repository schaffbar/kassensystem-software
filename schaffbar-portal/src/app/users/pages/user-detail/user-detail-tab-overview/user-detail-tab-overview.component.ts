import { Component, computed, inject, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';

import { TranslatePipe } from '@ngx-translate/core';

import { DangerZoneComponent } from '../../../../shared/components/danger-zone/danger-zone.component';
import { UserContactCardComponent } from '../../../components/user-contact-card/user-contact-card.component';
import {
  UserRfidTagCardComponent,
  UserRfidTagCardViewModel,
} from '../../../components/user-rfid-tag-card/user-rfid-tag-card.component';
import { UserToolCertificationsComponent } from '../../../components/user-tool-certifications/user-tool-certifications.component';
import { RfidTagAssignment, UnassignRfidTagCommand } from '../../../rfid-tag-assignment.model';
import {
  DeleteToolCertificationCommand,
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../../tool-certification.model';
import { User } from '../../../user.model';
import { UserDetailDialogsService } from '../user-detail-dialogs.service';
import { UserDetailStore } from '../user-detail.store';
import { toUserToolCertificationRowViewModels } from '../user-tool-certification-rows';

@Component({
  selector: 'schbar-user-detail-tab-overview',
  templateUrl: './user-detail-tab-overview.component.html',
  styleUrl: './user-detail-tab-overview.component.scss',
  imports: [
    MatButtonModule,
    TranslatePipe,
    DangerZoneComponent,
    UserContactCardComponent,
    UserRfidTagCardComponent,
    UserToolCertificationsComponent,
  ],
})
export class UserDetailTabOverviewComponent {
  protected readonly detailsStore = inject(UserDetailStore);
  private readonly dialogs = inject(UserDetailDialogsService);

  // the danger zone sends to the open session, which blocks deleting
  readonly openSessionRequested = output<void>();

  protected readonly rfidTagCard = computed(() => {
    const user = this.detailsStore.user();
    return user ? toUserRfidTagCardViewModel(user, this.detailsStore.rfidTagAssignment()) : null;
  });

  protected readonly certificationRows = computed(() =>
    toUserToolCertificationRowViewModels(
      this.detailsStore.toolCertifications(),
      this.detailsStore.tools(),
      this.detailsStore.userNames(),
    ),
  );

  // --------------------------------------------------------------------------
  // actions

  protected editUser(): void {
    this.dialogs.editUser();
  }

  protected assignRfidTag(): void {
    this.dialogs.assignRfidTag();
  }

  protected unassignRfidTag(command: UnassignRfidTagCommand): void {
    this.detailsStore.unassignRfidTag(command);
  }

  protected pauseToolCertification(command: PauseToolCertificationCommand): void {
    this.detailsStore.pauseToolCertification(command);
  }

  protected reactivateToolCertification(command: ReactivateToolCertificationCommand): void {
    this.detailsStore.reactivateToolCertification(command);
  }

  protected revokeToolCertification(command: RevokeToolCertificationCommand): void {
    this.detailsStore.revokeToolCertification(command);
  }

  protected deleteToolCertification(command: DeleteToolCertificationCommand): void {
    this.detailsStore.deleteToolCertification(command);
  }

  protected deleteUser(): void {
    this.dialogs.deleteUser();
  }
}

// ----------------------------------------------------------------------------
// mappers

function toUserRfidTagCardViewModel(user: User, assignment: RfidTagAssignment | null): UserRfidTagCardViewModel {
  return {
    userId: user.id,
    userName: `${user.firstName} ${user.lastName}`,
    assignment: assignment
      ? {
          assignmentType: assignment.assignmentType,
          status: assignment.status,
          rfidTagId: assignment.rfidTagId,
          assignmentDate: assignment.assignmentDate,
        }
      : null,
  };
}
