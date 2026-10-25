import { LowerCasePipe } from '@angular/common';
import { Component, inject, input, linkedSignal, output, signal } from '@angular/core';
import { form, FormField, FormRoot, readonly, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { CertificationRequirement, Tool, ToolArea, UpdateToolCommand } from '../../tool.model';
import { validateUniqueToolName } from '../../tool.validators';
import { ToolsStore } from '../../tools.store';

interface ToolInfoModel {
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
  private readonly toolsStore = inject(ToolsStore);

  readonly tool = input.required<Tool>();

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
    validateUniqueToolName(schema.name, this.toolsStore.entities, () => this.tool().id);
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

function toModel(tool: Tool): ToolInfoModel {
  return {
    name: tool.name,
    description: tool.description ?? '',
    area: tool.area,
    certificationRequirement: tool.certificationRequirement,
  };
}
