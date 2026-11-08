import { Component, computed, inject, Signal, signal } from '@angular/core';
import { form, FormField, FormRoot, required, submit } from '@angular/forms/signals';

import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';

import { TranslatePipe } from '@ngx-translate/core';

import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { FormDialogHeaderComponent } from '../../../shared/dialogs/form-dialog-header/form-dialog-header.component';
import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { CertificationRequirementDescriptionKeyPipe } from '../../certification-requirement-description-key.pipe';
import { UpdateToolCommand } from '../../tool.model';
import { TOOL_DESCRIPTION_MAX_LENGTH, validateToolDescription, validateUniqueToolName } from '../../tool.validators';
import { ToolAreaKeyPipe } from '../../tool-area-key.pipe';
import { CertificationRequirementBadgeComponent } from '../certification-requirement-badge/certification-requirement-badge.component';

export interface UpdateToolViewModel {
  id: string;
  name: string;
  description: string | null;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

export interface UpdateToolDialogData {
  tool: UpdateToolViewModel;
  activeCertificationsCount: number;
  // a signal, so the name check follows the tool list while the dialog is open
  existingTools: Signal<{ id: string; name: string }[]>;
}

interface UpdateToolFormModel {
  name: string;
  description: string;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
}

@Component({
  selector: 'schbar-update-tool-form',
  templateUrl: './update-tool-form.component.html',
  styleUrl: './update-tool-form.component.scss',
  imports: [
    FormField,
    FormRoot,
    FieldErrorComponent,
    FormDialogHeaderComponent,
    CertificationRequirementBadgeComponent,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
    TranslatePipe,
    CertificationRequirementDescriptionKeyPipe,
    ToolAreaKeyPipe,
  ],
  host: { class: 'form-dialog' },
})
export class UpdateToolFormComponent {
  protected readonly data: UpdateToolDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UpdateToolFormComponent, UpdateToolCommand>);

  protected readonly toolAreas = Object.values(ToolArea);
  protected readonly certificationRequirements = Object.values(CertificationRequirement);
  protected readonly descriptionMaxLength = TOOL_DESCRIPTION_MAX_LENGTH;

  private readonly model = signal<UpdateToolFormModel>(toUpdateToolFormModel(this.data.tool));

  protected readonly toolForm = form(this.model, (schema) => {
    required(schema.name);
    validateUniqueToolName(schema.name, this.data.existingTools, () => this.data.tool.id);
    validateToolDescription(schema.description);
  });

  // a changed requirement changes who may unlock the tool at the switch box
  protected readonly requirementImpactKey = computed(() => {
    const requirement = this.toolForm.certificationRequirement().value();
    if (requirement === this.data.tool.certificationRequirement) return null;
    return requirement === CertificationRequirement.Red
      ? 'tools.dialogs.editTool.impactToRed'
      : 'tools.dialogs.editTool.impactToYellow';
  });

  // --------------------------------------------------------------------------
  // actions

  protected saveTool(): void {
    submit(this.toolForm, async () => {
      const { name, description, area, certificationRequirement } = this.model();
      const command: UpdateToolCommand = {
        toolId: this.data.tool.id,
        name,
        description: description || undefined,
        area,
        certificationRequirement,
      };
      this.dialogRef.close(command);
    });
  }
}

// ----------------------------------------------------------------------------
// mappers

function toUpdateToolFormModel(tool: UpdateToolViewModel): UpdateToolFormModel {
  return {
    name: tool.name,
    description: tool.description ?? '',
    area: tool.area,
    certificationRequirement: tool.certificationRequirement,
  };
}
