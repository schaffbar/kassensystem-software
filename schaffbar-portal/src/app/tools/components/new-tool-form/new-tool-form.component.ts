import { Component, inject, Signal, signal } from '@angular/core';
import { form, FormField, FormRoot, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { CreateToolCommand } from '../../tool.model';
import { validateUniqueToolName } from '../../tool.validators';
import { CertificationRequirementKeyPipe } from '../../certification-requirement-key.pipe';
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
    MatInputModule,
    MatButtonModule,
    MatDialogModule,
    MatSelectModule,
    TranslatePipe,
    CertificationRequirementKeyPipe,
    ToolAreaKeyPipe,
  ],
})
export class NewToolFormComponent {
  private readonly data: NewToolDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<NewToolFormComponent, CreateToolCommand>);

  protected readonly toolAreas = Object.values(ToolArea);
  protected readonly certificationRequirements = Object.values(CertificationRequirement);

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
