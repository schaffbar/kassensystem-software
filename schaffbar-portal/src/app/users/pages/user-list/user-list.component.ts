import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { DIALOG_WIDTH } from '../../../shared/dialogs/dialog-constants';
import { NewUserFormComponent } from '../../components/new-user-form/new-user-form.component';
import { CreateUserCommand, DeleteUserCommand, User } from '../../user.model';
import { UsersStore } from '../../users.store';

@Component({
  selector: 'schbar-user-list',
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
  imports: [
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
  ],
})
export class UserListComponent {
  protected readonly store = inject(UsersStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  private readonly paginator = viewChild(MatPaginator);
  private readonly sort = viewChild(MatSort);

  protected readonly searchText = signal('');

  protected readonly displayedColumns = ['lastName', 'firstName', 'actions'];
  protected readonly usersCount = computed(() => this.store.users().length);
  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<User>(this.store.users());
    result.sort = this.sort() ?? null;
    result.paginator = this.paginator() ?? null;
    // part of the computed, so the filter survives reloading the users (MatTableDataSource default filter predicate)
    result.filter = this.searchText().trim().toLowerCase();
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

  protected searchUsers(term: string): void {
    this.searchText.set(term);
    this.paginator()?.firstPage();
  }

  protected clearSearch(): void {
    this.searchText.set('');
    this.paginator()?.firstPage();
  }

  protected openUserDetail(user: User): void {
    this.router.navigate([ROUTE.USERS, user.id]);
  }

  protected createUser(): void {
    const dialogRef = this.dialog.open<NewUserFormComponent, void, CreateUserCommand>(NewUserFormComponent, {
      minWidth: DIALOG_WIDTH.large,
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.store.createUser(command);
      }
    });
  }

  protected deleteUser(event: Event, user: User): void {
    event.stopPropagation();

    this.confirmDeleteDialog
      .open({
        title: 'users.dialogs.deleteUser.title',
        entity: user.firstName + ' ' + user.lastName,
        message: 'users.dialogs.deleteUser.message',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteUserCommand = { userId: user.id };
          this.store.deleteUser(command);
        }
      });
  }
}
