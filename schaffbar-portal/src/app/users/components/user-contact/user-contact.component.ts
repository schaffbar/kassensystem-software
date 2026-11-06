import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { email, form, FormField, FormRoot, readonly, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { UpdateUserContactCommand } from '../../user.model';

export interface UserContactViewModel {
  id: string;
  email: string;
  phone: string | null;
}

interface ContactFormModel {
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
  readonly userContact = input.required<UserContactViewModel>();

  readonly contactChanged = output<UpdateUserContactCommand>();

  protected readonly isEditing = signal(false);

  // follows the userContact input
  private readonly model = linkedSignal(() => toModel(this.userContact()));

  protected readonly contactForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
    required(schema.email);
    email(schema.email);
  });

  // --------------------------------------------------------------------------
  // actions

  protected editContact(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.contactForm.email().focusBoundControl();
  }

  protected discardContactChanges(): void {
    this.model.set(toModel(this.userContact()));
    this.contactForm().reset();
    this.isEditing.set(false);
  }

  protected saveContact(): void {
    submit(this.contactForm, async () => {
      const { email, phone } = this.model();
      const command: UpdateUserContactCommand = { id: this.userContact().id, email, phone: phone || undefined };
      this.contactChanged.emit(command);
      this.isEditing.set(false);
    });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toModel(userContact: UserContactViewModel): ContactFormModel {
  return { email: userContact.email, phone: userContact.phone ?? '' };
}
