import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

// a parent page in the breadcrumb trail – the current page is the title itself
export interface BreadcrumbViewModel {
  // translated
  label: string;
  link: string;
}

@Component({
  selector: 'schbar-page-header',
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
  imports: [RouterLink, MatIconModule, TranslatePipe],
})
export class PageHeaderComponent {
  // translated, like all texts of the header
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly breadcrumbs = input<BreadcrumbViewModel[]>([]);
}
