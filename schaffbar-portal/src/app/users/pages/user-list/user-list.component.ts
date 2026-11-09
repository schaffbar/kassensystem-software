import { Component, computed, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import {
  FilterChipsComponent,
  FilterChipViewModel,
} from '../../../shared/components/filter-chips/filter-chips.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { RfidTagAssignmentType } from '../../../shared/models/rfid-tag-assignment-type';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { RfidTagIdPipe } from '../../../shared/pipes/rfid-tag-id.pipe';
import { SNACKBAR_DURATION_MS } from '../../../shared/snack-bar/snack-bar-constants';
import { AgeGroupKeyPipe } from '../../age-group-key.pipe';
import { NewUserDialogData, NewUserFormComponent } from '../../components/new-user-form/new-user-form.component';
import { RfidTagAssignmentTypeKeyPipe } from '../../rfid-tag-assignment-type-key.pipe';
import { CreateUserCommand, DeleteUserCommand, hasTemporaryRfidTag, UserListItem } from '../../user.model';
import { UsersStore } from '../../users.store';

type UserFilter = 'all' | 'inWorkshop' | 'clubMembers' | 'temporaryRfidTag';

const FILTERS: Record<UserFilter, (user: UserListItem) => boolean> = {
  all: () => true,
  inWorkshop: (user) => user.inWorkshop,
  clubMembers: (user) => user.clubMember,
  temporaryRfidTag: hasTemporaryRfidTag,
};

@Component({
  selector: 'schbar-user-list',
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    TranslatePipe,
    PageHeaderComponent,
    EmptyStateComponent,
    FilterChipsComponent,
    RowMenuComponent,
    StatusBadgeComponent,
    AgeGroupKeyPipe,
    RfidTagAssignmentTypeKeyPipe,
    OrDashPipe,
    RfidTagIdPipe,
  ],
})
export class UserListComponent {
  protected readonly store = inject(UsersStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly paginator = viewChild(MatPaginator);

  protected readonly searchText = signal('');
  protected readonly filter = signal<string>('all');

  protected readonly displayedColumns = ['name', 'ageGroup', 'rfidTag', 'inWorkshop', 'actions'];
  protected readonly temporary = RfidTagAssignmentType.Temporary;

  private readonly filterLabels = toSignal<Record<string, string>>(
    this.translate.stream(Object.keys(FILTERS).map(toFilterLabelKey)),
  );

  protected readonly usersCount = computed(() => this.store.users().length);
  protected readonly isEmpty = computed(
    () => !this.store.isLoading() && !this.store.hasLoadError() && this.usersCount() === 0,
  );
  protected readonly subtitleParams = computed(() => ({
    count: this.usersCount(),
    inWorkshop: this.store.users().filter(FILTERS.inWorkshop).length,
  }));

  // the counts ignore the search, so they tell how many users each filter has at all
  protected readonly filterChips = computed<FilterChipViewModel[]>(() =>
    (Object.keys(FILTERS) as UserFilter[]).map((id) => ({
      id,
      label: this.filterLabels()?.[toFilterLabelKey(id)] ?? '',
      count: this.store.users().filter(FILTERS[id]).length,
    })),
  );

  private readonly shownUsers = computed(() => {
    const matchesFilter = FILTERS[this.filter() as UserFilter] ?? FILTERS.all;
    const searchText = toComparable(this.searchText());
    return this.store
      .users()
      .filter(matchesFilter)
      .filter((user) => !searchText || toSearchText(user).includes(searchText))
      .sort(byName);
  });
  protected readonly hasNoResults = computed(() => this.usersCount() > 0 && this.shownUsers().length === 0);

  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<UserListItem>(this.shownUsers());
    result.paginator = this.paginator() ?? null;
    return result;
  });

  constructor() {
    this.store.reloadUsers();
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadUsers(): void {
    this.store.reloadUsers();
  }

  protected searchUsers(searchText: string): void {
    this.searchText.set(searchText);
  }

  protected clearSearch(): void {
    this.searchText.set('');
  }

  protected openUserDetail(user: UserListItem): void {
    this.router.navigate([ROUTE.USERS, user.id]);
  }

  protected createUser(): void {
    const dialogRef = this.dialog.open<NewUserFormComponent, NewUserDialogData, CreateUserCommand>(
      NewUserFormComponent,
      {
        ...FORM_DIALOG_CONFIG,
        minWidth: DIALOG_WIDTH.medium,
        data: { existingUsers: this.store.users },
      },
    );

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.store.createUser(command);
      }
    });
  }

  protected deleteUser(user: UserListItem): void {
    this.store
      .canDeleteUser(user.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((canDelete) => {
        if (canDelete) {
          this.confirmUserDeletion(user);
        } else {
          this.showUserHasOpenSession(user);
        }
      });
  }

  // --------------------------------------------------------------------------
  // helper

  private confirmUserDeletion(user: UserListItem): void {
    this.confirmDeleteDialog
      .open({
        title: 'users.dialogs.deleteUser.title',
        entity: `${user.firstName} ${user.lastName}`,
        message: 'users.dialogs.deleteUser.message',
        confirmLabel: 'users.dialogs.deleteUser.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteUserCommand = { userId: user.id };
          this.store.deleteUser(command);
        }
      });
  }

  private showUserHasOpenSession(user: UserListItem): void {
    this.snackBar.open(
      this.translate.instant('users.dialogs.deleteUser.openSession', { name: `${user.firstName} ${user.lastName}` }),
      this.translate.instant('shared.actions.close'),
      { duration: SNACKBAR_DURATION_MS },
    );
  }
}

// ----------------------------------------------------------------------------
// helper

function toFilterLabelKey(filter: string): string {
  return `users.pages.userList.filters.${filter}`;
}

// a tag ID is shown in groups ("04A3 1F9C") – searching works with and without the spaces
function toComparable(text: string): string {
  return text.toLowerCase().replace(/\s+/g, '');
}

function toSearchText(user: UserListItem): string {
  return toComparable([user.firstName, user.lastName, user.email, user.rfidTag?.rfidTagId].join(' '));
}

function byName(a: UserListItem, b: UserListItem): number {
  return a.lastName.localeCompare(b.lastName, 'de') || a.firstName.localeCompare(b.firstName, 'de');
}
