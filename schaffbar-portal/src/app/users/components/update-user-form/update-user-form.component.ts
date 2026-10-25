import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { User } from '../../shared/models/user.model';

@Component({
  selector: 'schbar-update-user-form',
  templateUrl: './update-user-form.component.html',
  styleUrl: './update-user-form.component.scss',
  imports: [
    ReactiveFormsModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatDatepickerModule,
    TranslatePipe,
  ],
  providers: [provideNativeDateAdapter()],
})
export class UpdateUserFormComponent {
  private readonly user: User = inject(MAT_DIALOG_DATA);

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dialogRef = inject(MatDialogRef<UpdateUserFormComponent>);

  protected readonly userForm = this.fb.group({
    firstName: [this.user.firstName, Validators.required],
    lastName: [this.user.lastName, Validators.required],
    dateOfBirth: [this.user.dateOfBirth, Validators.required],
    clubMember: [this.user.clubMember],
  });

  // TODO: replace with a moment.js implementation
  private readonly _currentYear = new Date().getFullYear();
  protected readonly minDate = new Date(this._currentYear - 100, 0, 1);
  protected readonly maxDate = new Date(this._currentYear - 1, 11, 31);

  protected save() {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.dialogRef.close(this.userForm.value);
  }
}
