import { Component, input, model } from '@angular/core';

import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'schbar-toggle-chip',
  templateUrl: './toggle-chip.component.html',
  imports: [MatIconModule],
})
export class ToggleChipComponent {
  // translated
  readonly label = input.required<string>();
  readonly count = input<number | null>(null);
  readonly icon = input<string | null>(null);

  // whether the filter is on
  readonly pressed = model.required<boolean>();
}
