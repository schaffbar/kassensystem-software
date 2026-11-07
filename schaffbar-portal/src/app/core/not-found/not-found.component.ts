import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../app.routes';

@Component({
  selector: 'schbar-not-found',
  templateUrl: './not-found.component.html',
  styleUrl: './not-found.component.scss',
  imports: [RouterLink, MatButtonModule, TranslatePipe],
})
export class NotFoundComponent {
  protected readonly dashboardRoute = `/${ROUTE.DASHBOARD}`;
}
