import { Component, input, model } from '@angular/core';

export interface FilterChipViewModel {
  id: string;
  // translated
  label: string;
  count: number;
}

@Component({
  selector: 'schbar-filter-chips',
  templateUrl: './filter-chips.component.html',
  styleUrl: './filter-chips.component.scss',
  host: { role: 'group', '[attr.aria-label]': 'label()' },
})
export class FilterChipsComponent {
  // translated name of the group for screen readers, e.g. "Filter"
  readonly label = input.required<string>();
  readonly filterChips = input.required<FilterChipViewModel[]>();

  // id of the active filter
  readonly selected = model.required<string>();
}
