import { Component, computed, input } from '@angular/core';
import {
  MaxLengthValidationError,
  MaxValidationError,
  MinLengthValidationError,
  MinValidationError,
  ReadonlyFieldTree,
  ValidationError,
} from '@angular/forms/signals';

import { TranslatePipe } from '@ngx-translate/core';

interface ErrorMessage {
  key: string;
  value?: number;
}

/**
 * Shows the first validation error of a signal form field as translated text.
 *
 * Place it inside `<mat-error>` – Material only renders the error slot while the field is invalid and touched:
 * `<mat-error><schbar-field-error [field]="myForm.name" label="..." /></mat-error>`
 *
 * Built-in validators are translated by their `kind` (`shared.errors.*`). Custom errors provide their own
 * translation key via `message`, e.g. `{ kind: 'nameExists', message: 'tools.dialogs.addTool.errors.nameExists' }`.
 */
@Component({
  selector: 'schbar-field-error',
  templateUrl: './field-error.component.html',
  styleUrl: './field-error.component.scss',
  imports: [TranslatePipe],
})
export class FieldErrorComponent {
  readonly field = input.required<ReadonlyFieldTree<unknown>>();
  /** Translation key of the field label, used in messages like "<label> ist erforderlich". */
  readonly label = input.required<string>();

  protected readonly message = computed(() => {
    const fieldTree = this.field(); // read the input signal, e.g. `toolForm.name`
    const fieldState = fieldTree(); // calling a field tree returns its state (errors, touched, …)
    const error = fieldState.errors()[0];
    return error ? toErrorMessage(error) : undefined;
  });
}

function toErrorMessage(error: ValidationError): ErrorMessage {
  if (error.message) return { key: error.message };

  if (error instanceof MinLengthValidationError) return { key: 'shared.errors.minLength', value: error.minLength };
  if (error instanceof MaxLengthValidationError) return { key: 'shared.errors.maxLength', value: error.maxLength };
  if (error instanceof MinValidationError) return { key: 'shared.errors.min', value: error.min };
  if (error instanceof MaxValidationError) return { key: 'shared.errors.max', value: error.max };

  switch (error.kind) {
    case 'required':
      return { key: 'shared.errors.required' };
    case 'email':
      return { key: 'shared.errors.email' };
    default:
      return { key: 'shared.errors.invalid' };
  }
}
