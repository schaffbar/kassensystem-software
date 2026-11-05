import { Component, computed, inject, input, linkedSignal } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TranslatePipe } from '@ngx-translate/core';

import { DIALOG_WIDTH } from '../../../shared/dialogs/dialog-constants';
import {
  UpdateUserDialogData,
  UpdateUserFormComponent,
} from '../../components/update-user-form/update-user-form.component';
import {
  UserToolCertificationRowViewModel,
  UserToolCertificationsComponent,
} from '../../components/user-tool-certifications/user-tool-certifications.component';
import { UserAddressComponent } from '../../components/user-address/user-address.component';
import { UserDetailHeaderComponent } from '../../components/user-detail-header/user-detail-header.component';
import { UserContactComponent } from '../../components/user-contact/user-contact.component';
import { AssignRfidTagCommand, UnassignRfidTagCommand } from '../../rfid-tag-assignment.model';
import {
  PauseToolCertificationCommand,
  ReactivateToolCertificationCommand,
  RevokeToolCertificationCommand,
  ToolCertification,
} from '../../tool-certification.model';
import { Tool } from '../../tool.model';
import { UpdateUserAddressCommand, UpdateUserCommand, UpdateUserContactCommand } from '../../user.model';
import { UserDetailStore } from './user-detail.store';
import { UserOpenSessionComponent } from '../user-open-session/user-open-session.component';

@Component({
  selector: 'schbar-user-detail',
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.scss',
  imports: [
    UserDetailHeaderComponent,
    UserContactComponent,
    UserAddressComponent,
    UserOpenSessionComponent,
    UserToolCertificationsComponent,
    MatTabsModule,
    MatProgressBarModule,
    TranslatePipe,
  ],
  providers: [UserDetailStore],
})
export class UserDetailComponent {
  private readonly dialog = inject(MatDialog);
  protected readonly detailsStore = inject(UserDetailStore);

  // Info: id and tab inputs are coming from the route parameters.
  readonly id = input.required<string>();
  readonly tab = input<string>();

  protected readonly selectedTabIndex = linkedSignal<number>(() => (this.tab() === 'open-session' ? 1 : 0));
  protected readonly certificationRows = computed(() =>
    toCertificationRowViewModels(this.detailsStore.toolCertifications(), this.detailsStore.tools()),
  );

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
        minWidth: DIALOG_WIDTH.large,
        disableClose: true,
      },
    );

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.detailsStore.updateUser(command);
      }
    });
  }

  protected onAssignRfidTag(command: AssignRfidTagCommand): void {
    this.detailsStore.assignRfidTag(command);
  }

  protected onUnassignRfidTag(command: UnassignRfidTagCommand): void {
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

function toCertificationRowViewModels(
  certifications: ToolCertification[],
  tools: Tool[],
): UserToolCertificationRowViewModel[] {
  const toolNames = new Map(tools.map((tool) => [tool.id, tool.name]));
  return certifications.map((certification) => ({
    customerId: certification.customerId,
    toolId: certification.toolId,
    toolName: toolNames.get(certification.toolId) ?? null,
    status: certification.status,
    certifiedAt: certification.certifiedAt,
  }));
}
