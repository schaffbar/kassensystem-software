import { Component, input, model } from '@angular/core';

import { MatIconModule } from '@angular/material/icon';

export interface SegmentViewModel {
  id: string;
  // translated
  label: string;
  count?: number;
  icon?: string;
}

@Component({
  selector: 'schbar-segmented-control',
  templateUrl: './segmented-control.component.html',
  styleUrl: './segmented-control.component.scss',
  imports: [MatIconModule],
  host: { role: 'group', '[attr.aria-label]': 'label()' },
})
export class SegmentedControlComponent {
  // translated name of the group for screen readers, e.g. "Nach Bereich filtern"
  readonly label = input.required<string>();
  readonly segments = input.required<SegmentViewModel[]>();

  // id of the active segment
  readonly selected = model.required<string>();
}
