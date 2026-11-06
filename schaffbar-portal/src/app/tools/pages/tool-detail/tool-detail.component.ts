import { Component, inject, input } from '@angular/core';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TranslatePipe } from '@ngx-translate/core';

import { ToolsStore } from '../../tools.store';
import { ToolDetailTabCertificationsComponent } from './tool-detail-tab-certifications/tool-detail-tab-certifications.component';
import { ToolDetailTabConfigurationComponent } from './tool-detail-tab-configuration/tool-detail-tab-configuration.component';
import { ToolDetailTabInfoComponent } from './tool-detail-tab-info/tool-detail-tab-info.component';
import { ToolDetailStore } from './tool-detail.store';

@Component({
  selector: 'schbar-tool-detail',
  templateUrl: './tool-detail.component.html',
  styleUrl: './tool-detail.component.scss',
  imports: [
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    TranslatePipe,
    ToolDetailTabInfoComponent,
    ToolDetailTabCertificationsComponent,
    ToolDetailTabConfigurationComponent,
  ],
  providers: [ToolDetailStore],
})
export class ToolDetailComponent {
  protected readonly detailsStore = inject(ToolDetailStore);
  private readonly toolsStore = inject(ToolsStore);

  readonly id = input.required<string>();

  constructor() {
    this.detailsStore.setToolId(this.id);
    this.toolsStore.reloadTools();
  }

  // --------------------------------------------------------------------------
  // actions

  protected reloadTool(): void {
    this.detailsStore.reloadTool();
    this.toolsStore.reloadTools();
  }
}
