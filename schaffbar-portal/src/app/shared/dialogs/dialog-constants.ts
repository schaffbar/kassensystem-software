import { MatDialogConfig } from '@angular/material/dialog';

export const DIALOG_WIDTH = {
  small: '400px',
  medium: '600px',
  large: '800px',
} as const;

// options of a form dialog (spread into `MatDialog.open`): at the top of the window like in the prototype, so a
// growing dialog does not jump; the focus goes to the first field, not to the close button in the header
export const FORM_DIALOG_CONFIG = {
  autoFocus: 'input, textarea, mat-select',
  position: { top: '48px' },
  disableClose: true,
} as const satisfies MatDialogConfig;
