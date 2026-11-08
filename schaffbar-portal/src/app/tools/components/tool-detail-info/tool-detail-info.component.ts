import { Component, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

import { CertificationRequirement } from '../../../shared/models/certification-requirement';
import { ToolArea } from '../../../shared/models/tool-area';
import { CertificationRequirementDescriptionKeyPipe } from '../../certification-requirement-description-key.pipe';
import { ToolAreaKeyPipe } from '../../tool-area-key.pipe';
import { CertificationRequirementBadgeComponent } from '../certification-requirement-badge/certification-requirement-badge.component';

export interface ToolDetailInfoViewModel {
  description: string | null;
  area: ToolArea;
  certificationRequirement: CertificationRequirement;
  activeCertificationsCount: number;
}

@Component({
  selector: 'schbar-tool-detail-info',
  templateUrl: './tool-detail-info.component.html',
  styleUrl: './tool-detail-info.component.scss',
  imports: [
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    ToolAreaKeyPipe,
    CertificationRequirementDescriptionKeyPipe,
    CertificationRequirementBadgeComponent,
  ],
  host: { class: 'card' },
})
export class ToolDetailInfoComponent {
  readonly toolDetailInfo = input.required<ToolDetailInfoViewModel>();

  readonly editRequested = output<void>();
}
