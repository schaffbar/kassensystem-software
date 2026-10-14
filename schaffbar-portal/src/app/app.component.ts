import { Component, inject, ChangeDetectionStrategy } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';

import { NavigationComponent } from './core/navigation/navigation.component';

@Component({
  selector: 'schbar-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [NavigationComponent],
})
export class AppComponent {
  private translate = inject(TranslateService);

  protected title = 'schaffbar-portal';

  constructor() {
    this.translate.addLangs(['de', 'en']);
    this.translate.setFallbackLang('de');
    this.translate.use('de');
  }
}
