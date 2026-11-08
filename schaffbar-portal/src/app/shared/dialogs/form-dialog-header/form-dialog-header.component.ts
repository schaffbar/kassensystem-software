import { Component, input } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'schbar-form-dialog-header',
  templateUrl: './form-dialog-header.component.html',
  styleUrl: './form-dialog-header.component.scss',
  imports: [MatButtonModule, MatDialogModule, MatIconModule, TranslatePipe],
})
export class FormDialogHeaderComponent {
  // translated, like the subtitle
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
}
