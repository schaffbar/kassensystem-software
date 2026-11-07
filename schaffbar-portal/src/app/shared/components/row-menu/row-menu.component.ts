import { Component, input } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

@Component({
  selector: 'schbar-row-menu',
  templateUrl: './row-menu.component.html',
  imports: [MatButtonModule, MatIconModule, MatMenuModule],
})
export class RowMenuComponent {
  // accessible name of the ⋯ button, translated, e.g. "Aktionen für Bandsäge"
  readonly label = input.required<string>();
}
