import { Component, input } from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'schbar-danger-zone',
  templateUrl: './danger-zone.component.html',
  styleUrl: './danger-zone.component.scss',
  imports: [TranslatePipe],
  host: { class: 'card' },
})
export class DangerZoneComponent {
  // translated: what the destructive action removes
  readonly message = input.required<string>();
}
