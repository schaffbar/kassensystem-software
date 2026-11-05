import { LowerCasePipe } from '@angular/common';
import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, FormRoot, readonly, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { UpdateToolCommand } from '../../tool.model';
import { validateUniqueToolName } from '../../tool.validators';

export interface ToolInfoViewModel {
  id: string;
  name: string;
  description: string | null;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

// all tools, incl. this one – for the unique name check
export interface ExistingToolViewModel {
  id: string;
  name: string;
}

interface ToolFormModel {
  name: string;
  description: string;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

@Component({
  selector: 'schbar-tool-detail-info',
  templateUrl: './tool-detail-info.component.html',
  styleUrl: './tool-detail-info.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    LowerCasePipe,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    TranslatePipe,
  ],
})
export class ToolDetailInfoComponent {
  readonly tool = input.required<ToolInfoViewModel>();
  readonly existingTools = input.required<ExistingToolViewModel[]>();

  readonly toolUpdated = output<UpdateToolCommand>();

  protected readonly isEditing = signal(false);

  protected readonly toolAreas = Object.values(ToolArea);
  protected readonly certificationRequirements = Object.values(CertificationRequirement);

  // follows the tool input
  private readonly model = linkedSignal(() => toModel(this.tool()));

  protected readonly toolForm = form(this.model, (schema) => {
    // readonly is inherited by all fields
    readonly(schema, { when: () => !this.isEditing() });
    required(schema.name);
    validateUniqueToolName(schema.name, this.existingTools, () => this.tool().id);
  });

  protected onEdit(): void {
    this.isEditing.set(true);
    // the edit button gets disabled, so move the focus into the form – this also makes Enter submit the form
    this.toolForm.name().focusBoundControl();
  }

  protected onCancel(): void {
    this.model.set(toModel(this.tool()));
    this.toolForm().reset();
    this.isEditing.set(false);
  }

  protected onSave(): void {
    submit(this.toolForm, async () => {
      const { name, description, area, certificationRequirement } = this.model();
      const command: UpdateToolCommand = {
        id: this.tool().id,
        name,
        description,
        area,
        certificationRequirement,
      };
      this.toolUpdated.emit(command);
      this.isEditing.set(false);
    });
  }
}

function toModel(tool: ToolInfoViewModel): ToolFormModel {
  return {
    name: tool.name,
    description: tool.description ?? '',
    area: tool.area,
    certificationRequirement: tool.certificationRequirement,
  };
}
