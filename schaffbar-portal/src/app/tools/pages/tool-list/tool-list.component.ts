import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ROUTE } from '../../../app.routes';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RowMenuComponent } from '../../../shared/components/row-menu/row-menu.component';
import {
  SegmentedControlComponent,
  SegmentViewModel,
} from '../../../shared/components/segmented-control/segmented-control.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ToggleChipComponent } from '../../../shared/components/toggle-chip/toggle-chip.component';
import { ConfirmDeleteDialogService } from '../../../shared/dialogs/confirm-delete-dialog/confirm-delete-dialog.service';
import { DIALOG_WIDTH, FORM_DIALOG_CONFIG } from '../../../shared/dialogs/dialog-constants';
import { ToolArea } from '../../../shared/models/tool-area';
import { CertificationRequirementBadgeComponent } from '../../../shared/components/certification-requirement-badge/certification-requirement-badge.component';
import { NewToolDialogData, NewToolFormComponent } from '../../components/new-tool-form/new-tool-form.component';
import { CreateToolCommand, DeleteToolCommand, isHardwareSetUp, ToolListItem } from '../../tool.model';
import { ToolsStore } from '../../tools.store';
import { ToolAreaKeyPipe } from '../../../shared/pipes/tool-area-key.pipe';
import { WlanRelaisTypeKeyPipe } from '../../wlan-relais-type-key.pipe';

const ALL_AREAS = 'all';
const AREAS = Object.values(ToolArea);

type ToolListView = 'cards' | 'list';

@Component({
  selector: 'schbar-tool-list',
  templateUrl: './tool-list.component.html',
  styleUrl: './tool-list.component.scss',
  imports: [
    NgTemplateOutlet,
    RouterLink,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTableModule,
    TranslatePipe,
    PageHeaderComponent,
    EmptyStateComponent,
    RowMenuComponent,
    SegmentedControlComponent,
    StatusBadgeComponent,
    ToggleChipComponent,
    CertificationRequirementBadgeComponent,
    ToolAreaKeyPipe,
    WlanRelaisTypeKeyPipe,
  ],
})
export class ToolListComponent {
  protected readonly store = inject(ToolsStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly confirmDeleteDialog = inject(ConfirmDeleteDialogService);

  protected readonly searchText = signal('');
  protected readonly area = signal<string>(ALL_AREAS);
  protected readonly view = signal<string>('cards' satisfies ToolListView);
  protected readonly notSetUpOnly = signal(false);

  protected readonly isHardwareSetUp = isHardwareSetUp;

  protected readonly listColumns = ['name', 'certificationRequirement', 'state', 'hardware', 'actions'];

  private readonly labels = toSignal<Record<string, string>>(
    this.translate.stream([
      'tools.pages.toolList.areaFilter.all',
      'tools.pages.toolList.views.cards',
      'tools.pages.toolList.views.list',
      ...AREAS.map(toAreaKey),
    ]),
  );

  protected readonly toolsCount = computed(() => this.store.tools().length);
  protected readonly isEmpty = computed(
    () => !this.store.isLoading() && !this.store.hasLoadError() && this.toolsCount() === 0,
  );
  protected readonly notSetUpCount = computed(() => this.store.tools().filter((tool) => !isHardwareSetUp(tool)).length);
  protected readonly subtitle = computed(() => ({
    key: this.toolsCount() === 1 ? 'tools.pages.toolList.subtitleOne' : 'tools.pages.toolList.subtitle',
    params: {
      count: this.toolsCount(),
      inUse: this.store.tools().filter((tool) => tool.usedBy).length,
    },
  }));

  // the counts ignore the search, so they tell how many tools each area has at all
  protected readonly areaSegments = computed<SegmentViewModel[]>(() => {
    const tools = this.store.tools();
    return [
      { id: ALL_AREAS, label: this.label('tools.pages.toolList.areaFilter.all'), count: tools.length },
      ...AREAS.map((area) => ({
        id: area,
        label: this.label(toAreaKey(area)),
        count: tools.filter((tool) => tool.area === area).length,
      })),
    ];
  });

  protected readonly viewSegments = computed<SegmentViewModel[]>(() => [
    { id: 'cards', label: this.label('tools.pages.toolList.views.cards'), icon: 'grid_view' },
    { id: 'list', label: this.label('tools.pages.toolList.views.list'), icon: 'view_list' },
  ]);

  protected readonly shownTools = computed(() => {
    const area = this.area();
    const notSetUpOnly = this.notSetUpOnly();
    const searchText = this.searchText().trim().toLowerCase();
    return this.store
      .tools()
      .filter((tool) => area === ALL_AREAS || tool.area === area)
      .filter((tool) => !notSetUpOnly || !isHardwareSetUp(tool))
      .filter((tool) => !searchText || tool.name.toLowerCase().includes(searchText))
      .sort((a, b) => a.name.localeCompare(b.name, 'de'));
  });

  // cards are grouped by area, in the order of the areas
  protected readonly areaGroups = computed(() =>
    AREAS.map((area) => ({ area, tools: this.shownTools().filter((tool) => tool.area === area) })).filter(
      (group) => group.tools.length > 0,
    ),
  );

  protected readonly hasNoResults = computed(() => this.toolsCount() > 0 && this.shownTools().length === 0);

  constructor() {
    this.store.reloadTools();
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadTools(): void {
    this.store.reloadTools();
  }

  protected searchTools(searchText: string): void {
    this.searchText.set(searchText);
  }

  protected clearSearch(): void {
    this.searchText.set('');
  }

  protected openToolDetail(tool: ToolListItem): void {
    this.router.navigate([ROUTE.TOOLS, tool.id]);
  }

  protected createTool(): void {
    const dialogRef = this.dialog.open<NewToolFormComponent, NewToolDialogData, CreateToolCommand>(
      NewToolFormComponent,
      {
        ...FORM_DIALOG_CONFIG,
        minWidth: DIALOG_WIDTH.medium,
        data: { existingTools: this.store.tools },
      },
    );

    dialogRef.afterClosed().subscribe((command) => {
      if (command) {
        this.store.createTool(command);
      }
    });
  }

  protected deleteTool(tool: ToolListItem): void {
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

  // --------------------------------------------------------------------------
  // helper

  private label(key: string): string {
    return this.labels()?.[key] ?? '';
  }
}

// ----------------------------------------------------------------------------
// helper

function toAreaKey(area: ToolArea): string {
  return `shared.toolAreas.${area}`;
}
