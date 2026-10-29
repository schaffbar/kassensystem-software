import { LowerCasePipe } from '@angular/common';
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
import { ConfirmDialogService } from '../../../shared/components/confirm-delete/confirm-dialog.service';
import { OrDashPipe } from '../../../shared/pipes/or-dash.pipe';
import { NewToolFormComponent } from '../../components/new-tool-form/new-tool-form.component';
import { CreateToolCommand, DeleteToolCommand, Tool } from '../../tool.model';
import { ToolsStore } from '../../tools.store';

@Component({
  selector: 'schbar-tool-list',
  templateUrl: './tool-list.component.html',
  styleUrl: './tool-list.component.scss',
  imports: [
    LowerCasePipe,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatIconModule,
    TranslatePipe,
    OrDashPipe,
  ],
})
export class ToolListComponent {
  protected readonly store = inject(ToolsStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDialog = inject(ConfirmDialogService);

  private readonly paginator = viewChild(MatPaginator);
  private readonly sort = viewChild(MatSort);

  protected readonly displayedColumns = [
    'certificationRequirement',
    'name',
    'area',
    'description',
    'rfidReader',
    'wlanRelaisType',
    'actions',
  ];

  protected readonly toolsCount = computed(() => this.store.entities().length);
  protected readonly dataSource = computed(() => {
    const result = new MatTableDataSource<Tool>(this.store.entities());
    result.sort = this.sort() ?? null;
    result.paginator = this.paginator() ?? null;
    return result;
  });

  protected readonly isCreatingTool = this.store.createTool.isPending;

  constructor() {
    this.store.reloadTools();
  }

  protected toolDetails(tool: Tool): void {
    this.router.navigate([ROUTE.TOOLS, tool.id]);
  }

  protected reloadTools(): void {
    this.store.reloadTools();
  }

  protected newToolDialog(): void {
    const dialogRef = this.dialog.open<NewToolFormComponent, void, CreateToolCommand>(NewToolFormComponent, {
      minWidth: '600px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.store.createTool(command);
      }
    });
  }

  protected deleteToolDialog(event: Event, tool: Tool): void {
    event.stopPropagation();

    if (!tool || !tool.id) {
      console.warn('No tool selected for deletion');
      return;
    }

    this.confirmDialog
      .confirm({
        title: 'tools.dialogs.deleteTool.title',
        entity: tool.name,
        message: 'tools.dialogs.deleteTool.message',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          const command: DeleteToolCommand = { id: tool.id };
          this.store.deleteTool(command);
        }
      });
  }
}
