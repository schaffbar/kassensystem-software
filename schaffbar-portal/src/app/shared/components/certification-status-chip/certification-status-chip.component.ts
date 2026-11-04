import { Component, input } from '@angular/core';

import { MatChipsModule } from '@angular/material/chips';

import { TranslatePipe } from '@ngx-translate/core';

import { ToolCertificationStatus } from '../../models/tool-certification-status';

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
