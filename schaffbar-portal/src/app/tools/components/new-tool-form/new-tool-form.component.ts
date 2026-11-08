import { Component, inject, Signal, signal } from '@angular/core';
import { form, FormField, FormRoot, maxLength, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { CreateToolCommand } from '../../tool.model';
import { validateUniqueToolName } from '../../tool.validators';
import { CertificationRequirementBadgeComponent } from '../certification-requirement-badge/certification-requirement-badge.component';
import { CertificationRequirementDescriptionKeyPipe } from '../../certification-requirement-description-key.pipe';
import { ToolAreaKeyPipe } from '../../tool-area-key.pipe';

// all tools – for the unique name check
export interface ExistingToolViewModel {
  id: string;
  name: string;
}

export interface NewToolDialogData {
  // a signal, so the name check follows the tool list while the dialog is open
  existingTools: Signal<ExistingToolViewModel[]>;
}

// the database column holds 255 characters
export const DESCRIPTION_MAX_LENGTH = 255;

interface NewToolFormModel {
  name: string;
  description: string;
  area: ToolArea | '';
  certificationRequirement: CertificationRequirement | '';
}

@Component({
  selector: 'schbar-new-tool-form',
  templateUrl: './new-tool-form.component.html',
  styleUrl: './new-tool-form.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    FormDialogHeaderComponent,
    CertificationRequirementBadgeComponent,
    MatInputModule,
    MatButtonModule,
    MatDialogModule,
    MatRadioModule,
    TranslatePipe,
    CertificationRequirementDescriptionKeyPipe,
    ToolAreaKeyPipe,
  ],
  host: { class: 'form-dialog' },
})
export class NewToolFormComponent {
  private readonly data: NewToolDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<NewToolFormComponent, CreateToolCommand>);

  protected readonly toolAreas = Object.values(ToolArea);
  protected readonly certificationRequirements = Object.values(CertificationRequirement);
  protected readonly descriptionMaxLength = DESCRIPTION_MAX_LENGTH;

  private readonly model = signal<NewToolFormModel>({
    name: '',
    description: '',
    area: '',
    certificationRequirement: '',
  });

  protected readonly toolForm = form(this.model, (schema) => {
    required(schema.name);
    validateUniqueToolName(schema.name, this.data.existingTools);
    required(schema.area);
    required(schema.certificationRequirement);
    maxLength(schema.description, DESCRIPTION_MAX_LENGTH);
  });

  // --------------------------------------------------------------------------
  // actions

  protected saveTool(): void {
    submit(this.toolForm, async () => {
      const { name, description, area, certificationRequirement } = this.model();
      if (!area || !certificationRequirement) return;

      const command: CreateToolCommand = {
        name,
        description: description || undefined,
        area,
        certificationRequirement,
      };
      this.dialogRef.close(command);
    });
  }
}
