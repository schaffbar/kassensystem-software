import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { Router } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { ConfirmDeleteComponent } from '../../../shared/components/confirm-delete/confirm-delete.component';
import { NewUserFormComponent } from '../../components/new-user-form/new-user-form.component';
import { CreateUserCommand, DeleteUserCommand, User } from '../../shared/models/user.model';
import { UsersStore } from '../../shared/stores/users.store';

@Component({
  selector: 'schbar-user-list',
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
  imports: [
    FormField,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
  ],
})
export class UserListComponent {
  private readonly store = inject(UsersStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  private readonly paginator = viewChild(MatPaginator);
  private readonly sort = viewChild(MatSort);

  private readonly search = signal({ term: '' });
  // TODO: do I need to use form here
  protected readonly searchForm = form(this.search);

  protected readonly displayedColumns = ['lastName', 'firstName', 'actions'];
  protected readonly usersCount = computed(() => this.store.entities().length);
  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<User>(this.store.entities());
    result.sort = this.sort() ?? null;
    result.paginator = this.paginator() ?? null;
    // part of the computed, so the filter survives reloading the users (MatTableDataSource default filter predicate)
    result.filter = this.search().term.trim().toLowerCase();
    return result;
  });

  constructor() {
    this.store.loadAllUsers();
  }

  protected reloadUsers(): void {
    this.store.loadAllUsers();
  }

  protected onSearchInput(): void {
    this.paginator()?.firstPage();
  }

  protected clearSearch(): void {
    this.search.set({ term: '' });
    this.paginator()?.firstPage();
  }

  protected userDetails(user: User): void {
    this.router.navigate([ROUTE.USERS, user.id]);
  }

  protected newUserDialog(): void {
    const dialogRef = this.dialog.open<NewUserFormComponent, void, CreateUserCommand>(NewUserFormComponent, {
      minWidth: '800px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.store.createUser(command);
      }
    });
  }

  protected deleteUserDialog(event: Event, user: User): void {
    event.stopPropagation();

    if (!user || !user.id) {
      console.warn('No user selected for deletion');
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDeleteComponent, {
      data: {
        title: this.translate.instant('users.dialogs.deleteUser.title'),
        entity: user.firstName + ' ' + user.lastName,
        message: this.translate.instant('users.dialogs.deleteUser.message'),
      },
      minWidth: '600px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        const command: DeleteUserCommand = { id: user.id };
        this.store.deleteUser(command);
      }
    });
  }
}
