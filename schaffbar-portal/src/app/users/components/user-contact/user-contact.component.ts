import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { email, form, FormField, FormRoot, readonly, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { UpdateUserContactCommand, User } from '../../shared/models/user.model';

interface ContactModel {
  email: string;
  phone: string;
}

@Component({
  selector: 'schbar-user-contact',
  templateUrl: './user-contact.component.html',
  styleUrl: './user-contact.component.scss',
  imports: [FormField, FormRoot, FieldErrorComponent, MatInputModule, MatButtonModule, MatIconModule, TranslatePipe],
})
export class UserContactComponent {
  readonly user = input.required<User>();

  readonly contactChanged = output<UpdateUserContactCommand>();

  protected readonly isEditing = signal(false);

  // follows the user input
  private readonly model = linkedSignal(() => toModel(this.user()));

  protected readonly contactForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
    required(schema.email);
    email(schema.email);
  });

  protected onEdit(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.contactForm.email().focusBoundControl();
  }

  protected onCancel(): void {
    this.model.set(toModel(this.user()));
    this.contactForm().reset();
    this.isEditing.set(false);
  }

  protected onSave(): void {
    submit(this.contactForm, async () => {
      const { email, phone } = this.model();
      const command: UpdateUserContactCommand = { id: this.user().id, email, phone };
      this.contactChanged.emit(command);
      this.isEditing.set(false);
    });
  }
}

function toModel(user: User): ContactModel {
  return { email: user.email, phone: user.phone ?? '' };
}
