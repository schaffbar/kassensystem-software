import { Component, computed, inject, viewChild } from '@angular/core';
import { Router } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { DIALOG_WIDTH } from '../../../shared/dialogs/dialog-constants';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { CertificationRequirementDotComponent } from '../../components/certification-requirement-dot/certification-requirement-dot.component';
import { NewToolDialogData, NewToolFormComponent } from '../../components/new-tool-form/new-tool-form.component';
import { CreateToolCommand, DeleteToolCommand, Tool } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolAreaKeyPipe } from '../../tool-area-key.pipe';
import { WlanRelaisTypeKeyPipe } from '../../wlan-relais-type-key.pipe';

@Component({
  selector: 'schbar-tool-list',
  templateUrl: './tool-list.component.html',
  styleUrl: './tool-list.component.scss',
  imports: [
    CertificationRequirementDotComponent,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatIconModule,
    TranslatePipe,
    OrDashPipe,
    ToolAreaKeyPipe,
    WlanRelaisTypeKeyPipe,
  ],
})
export class ToolListComponent {
  protected readonly store = inject(ToolsStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  private readonly paginator = viewChild(MatPaginator);
  private readonly sort = viewChild(MatSort);

  protected readonly displayedColumns = [
    'certificationRequirement',
    'name',
    'area',
    'description',
    'rfidReaderId',
    'wlanRelaisType',
    'actions',
  ];

  protected readonly toolsCount = computed(() => this.store.tools().length);
  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<Tool>(this.store.tools());
    result.sort = this.sort() ?? null;
    result.paginator = this.paginator() ?? null;
    return result;
  });

  constructor() {
    this.store.reloadTools();
  }

  // --------------------------------------------------------------------------
  // actions

  protected openToolDetail(tool: Tool): void {
    this.router.navigate([ROUTE.TOOLS, tool.id]);
  }

  protected reloadTools(): void {
    this.store.reloadTools();
  }

  protected createTool(): void {
    const dialogRef = this.dialog.open<NewToolFormComponent, NewToolDialogData, CreateToolCommand>(
      NewToolFormComponent,
      {
        data: { existingTools: this.store.tools },
        minWidth: DIALOG_WIDTH.medium,
        disableClose: true,
      },
    );

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.store.createTool(command);
      }
    });
  }

  protected deleteTool(event: Event, tool: Tool): void {
    event.stopPropagation();

    this.confirmDeleteDialog
      .open({
        title: 'tools.dialogs.deleteTool.title',
        entity: tool.name,
        message: 'tools.dialogs.deleteTool.message',
        confirmLabel: 'tools.dialogs.deleteTool.confirm',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteToolCommand = { toolId: tool.id };
          this.store.deleteTool(command);
        }
      });
  }
}
