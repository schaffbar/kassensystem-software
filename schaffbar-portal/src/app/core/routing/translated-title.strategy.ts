import { inject, Service } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

const APP_NAME = 'Schaffbar';

/**
 * Sets the browser tab title from the `title` of the active route. The route title is a translation key
 * (e.g. `title: 'sidebar.tools'`), the tab shows "Werkzeuge · Schaffbar".
 */
@Service()
export class TranslatedTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly translate = inject(TranslateService);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const titleKey = this.buildTitle(snapshot);
    if (!titleKey) {
      this.title.setTitle(APP_NAME);
      return;
    }
    // get() waits for the translations to be loaded and completes afterwards
    this.translate.get(titleKey).subscribe((pageTitle: string) => this.title.setTitle(`${pageTitle} · ${APP_NAME}`));
  }
}
