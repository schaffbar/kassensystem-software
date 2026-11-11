import { Component, input } from '@angular/core';

// a key figure of the overview: "In der Werkstatt 7 – davon 2 unter 18"
@Component({
  selector: 'schbar-stat-tile',
  templateUrl: './stat-tile.component.html',
  styleUrl: './stat-tile.component.scss',
})
export class StatTileComponent {
  // translated, like the detail
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  // "3 / 18" – null shows the value alone
  readonly total = input<number | null>(null);
  readonly detail = input<string | null>(null);
}
