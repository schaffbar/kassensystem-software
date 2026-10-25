import { LowerCasePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, required, submit, validate } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { CertificationRequirement, CreateToolCommand, ToolArea } from '../../tool.model';
import { ToolsStore } from '../../tools.store';

interface NewToolModel {
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
    LowerCasePipe,
    MatInputModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatSelectModule,
    TranslatePipe,
  ],
})
export class NewToolFormComponent {
  private readonly dialogRef = inject(MatDialogRef<NewToolFormComponent, CreateToolCommand>);
  private readonly toolsStore = inject(ToolsStore);

  protected readonly toolAreas = Object.values(ToolArea);
  protected readonly certificationRequirements = Object.values(CertificationRequirement);

  private readonly model = signal<NewToolModel>({
    name: '',
    description: '',
    area: '',
    certificationRequirement: '',
  });

  protected readonly toolForm = form(this.model, (schema) => {
    required(schema.name);
    validate(schema.name, ({ value }) => {
      const name = value().toLowerCase();
      if (!name) return undefined;

      // reads the store signal, so the check re-runs when the tool list changes
      const exists = this.toolsStore.entities().some((tool) => tool.name.toLowerCase() === name);
      return exists ? { kind: 'nameExists', message: 'tools.dialogs.addTool.errors.nameExists' } : undefined;
    });
    required(schema.area);
    required(schema.certificationRequirement);
  });

  protected onSave(): void {
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
