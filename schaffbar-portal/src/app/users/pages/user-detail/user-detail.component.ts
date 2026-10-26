import { LowerCasePipe } from '@angular/common';
import { Component, computed, inject, input, linkedSignal } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TranslatePipe } from '@ngx-translate/core';

import { ToolsStore } from '../../../tools/tools.store';
import {
  UpdateUserDialogData,
  UpdateUserFormComponent,
} from '../../components/update-user-form/update-user-form.component';
import { UserToolCertificationsComponent } from '../../components/user-tool-certifications/user-tool-certifications.component';
import {
  AssignRfidTagCommand,
  RfidTagAssignmentStatus,
  RfidTagAssignmentType,
  UnassignRfidTagCommand,
} from '../../shared/models/rfid-tag-assignment.model';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
} from '../../shared/models/tool-certification.model';
import { UpdateUserAddressCommand, UpdateUserCommand, UpdateUserContactCommand } from '../../shared/models/user.model';
import { UserDetailStore } from '../../shared/stores/user-detail.store';
import { UserDetailInfoComponent } from '../user-detail-info/user-detail-info.component';
import { UserOpenSessionComponent } from '../user-open-session/user-open-session.component';

@Component({
  selector: 'schbar-user-detail',
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.scss',
  imports: [
    UserDetailInfoComponent,
    UserOpenSessionComponent,
    UserToolCertificationsComponent,
    MatTabsModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatProgressBarModule,
    TranslatePipe,
    LowerCasePipe,
  ],
  providers: [UserDetailStore],
})
export class UserDetailComponent {
  private readonly dialog = inject(MatDialog);
  protected readonly detailsStore = inject(UserDetailStore);
  private readonly toolsStore = inject(ToolsStore);

  // Info: id and tab inputs are coming from the route parameters.
  readonly id = input.required<string>();
  readonly tab = input<string>();

  protected readonly selectedTabIndex = linkedSignal<number>(() => (this.tab() === 'open-session' ? 1 : 0));
  protected readonly allTools = computed(() => this.toolsStore.entities());
  protected readonly RfidTagAssignmentStatus = RfidTagAssignmentStatus;

  constructor() {
    this.detailsStore.setUserId(this.id);
  }

  protected reloadUser(): void {
    this.detailsStore.reloadUser();
  }

  protected updateUser(): void {
    const user = this.detailsStore.user();
    if (!user) return;

    const dialogRef = this.dialog.open<UpdateUserFormComponent, UpdateUserDialogData, UpdateUserCommand>(
      UpdateUserFormComponent,
      {
        data: { user },
        minWidth: '800px',
        disableClose: true,
      },
    );

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.detailsStore.updateUser(command);
      }
    });
  }

  protected assignFixedRfidTag(): void {
    const command: AssignRfidTagCommand = { customerId: this.id(), assignmentType: RfidTagAssignmentType.Fixed };
    this.detailsStore.assignRfidTag(command);
  }

  protected assignTemporaryRfidTag(): void {
    const command: AssignRfidTagCommand = { customerId: this.id(), assignmentType: RfidTagAssignmentType.Temporary };
    this.detailsStore.assignRfidTag(command);
  }

  protected unassignRfidTag(): void {
    const command: UnassignRfidTagCommand = { customerId: this.id() };
    this.detailsStore.unassignRfidTag(command);
  }

  protected onAddressChanged(command: UpdateUserAddressCommand) {
    this.detailsStore.updateUserAddress(command);
  }

  protected onContactChanged(command: UpdateUserContactCommand) {
    this.detailsStore.updateUserContact(command);
  }

  protected onPauseCertification(command: PauseToolCertificationCommand): void {
    this.detailsStore.pauseCertification(command);
  }

  protected onReactivateCertification(command: ReactivateToolCertificationCommand): void {
    this.detailsStore.reactivateCertification(command);
  }

  protected onRevokeCertification(command: RevokeToolCertificationCommand): void {
    this.detailsStore.revokeCertification(command);
  }
}
