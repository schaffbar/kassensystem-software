import { Component, input } from '@angular/core';

import { MatChipsModule } from '@angular/material/chips';

import { TranslatePipe } from '@ngx-translate/core';

import { ToolCertificationStatus } from '../../../users/shared/models/tool-certification.model';

@Component({
  selector: 'schbar-certification-status-chip',
  templateUrl: './certification-status-chip.component.html',
  styleUrl: './certification-status-chip.component.scss',
  imports: [MatChipsModule, TranslatePipe],
})
export class CertificationStatusChipComponent {
  readonly status = input.required<ToolCertificationStatus>();

  protected readonly Status = ToolCertificationStatus;
}
