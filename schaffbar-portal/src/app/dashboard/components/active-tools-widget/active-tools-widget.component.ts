import { Component, computed, input, output } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';

export interface ToolStatusViewModel {
  id: string;
  name: string;
  // null while nobody uses the tool
  usedBy: { firstName: string; lastName: string } | null;
}

@Component({
  selector: 'schbar-active-tools-widget',
  templateUrl: './active-tools-widget.component.html',
  styleUrl: './active-tools-widget.component.scss',
  imports: [MatButtonModule, MatCardModule, MatTableModule, MatIconModule, TranslatePipe],
})
export class ActiveToolsWidgetComponent {
  readonly toolStatuses = input.required<ToolStatusViewModel[]>();

  readonly reload = output<void>();

  protected readonly displayedColumns = ['status', 'name', 'user'];
  protected readonly activeToolsCount = computed(() => this.toolStatuses().filter((tool) => tool.usedBy).length);
  protected readonly dataSource = computed(() => {
    // tools in use first
    const rows = [...this.toolStatuses()].sort((a, b) => Number(!!b.usedBy) - Number(!!a.usedBy));
    return new MatTableDataSource<ToolStatusViewModel>(rows);
  });

  protected reloadActiveTools(): void {
    this.reload.emit();
  }
}
