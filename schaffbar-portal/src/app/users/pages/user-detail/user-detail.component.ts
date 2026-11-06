import { Component, inject, input, linkedSignal } from '@angular/core';

import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TranslatePipe } from '@ngx-translate/core';

import { DIALOG_WIDTH } from '../../../shared/dialogs/dialog-constants';
import {
  UpdateUserDialogData,
  UpdateUserFormComponent,
} from '../../components/update-user-form/update-user-form.component';
import { UserDetailHeaderComponent } from '../../components/user-detail-header/user-detail-header.component';
import { AssignRfidTagCommand, UnassignRfidTagCommand } from '../../rfid-tag-assignment.model';
import { UpdateUserCommand } from '../../user.model';
import { UserDetailTabInfoComponent } from './user-detail-tab-info/user-detail-tab-info.component';
import { UserDetailTabSessionComponent } from './user-detail-tab-session/user-detail-tab-session.component';
import { UserDetailStore } from './user-detail.store';

@Component({
  selector: 'schbar-user-detail',
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.scss',
  imports: [
    UserDetailHeaderComponent,
    UserDetailTabInfoComponent,
    UserDetailTabSessionComponent,
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

  constructor() {
    this.detailsStore.setUserId(this.id);
  }

  // --------------------------------------------------------------------------
  // actions

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

  protected assignRfidTag(command: AssignRfidTagCommand): void {
    this.detailsStore.assignRfidTag(command);
  }

  protected unassignRfidTag(command: UnassignRfidTagCommand): void {
    this.detailsStore.unassignRfidTag(command);
  }
}
