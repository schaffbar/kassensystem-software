import { Component, input } from '@angular/core';

import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'schbar-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
  imports: [MatIconModule],
})
export class EmptyStateComponent {
  readonly icon = input.required<string>();
  // translated, like the message
  readonly title = input.required<string>();
  readonly message = input<string | null>(null);
}
