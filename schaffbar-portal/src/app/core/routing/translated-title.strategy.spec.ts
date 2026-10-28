import { Component, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, TitleStrategy } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { TranslatedTitleStrategy } from './translated-title.strategy';

@Component({ template: '' })
class EmptyComponent {}

describe('TranslatedTitleStrategy', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideTranslateService(),
        provideRouter([
          { path: 'tools', title: 'sidebar.tools', component: EmptyComponent },
          { path: 'untitled', component: EmptyComponent },
        ]),
        { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
      ],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('de', { sidebar: { tools: 'Werkzeuge' } });
    translate.use('de');
  });

  it('should show the translated route title with the app name', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/tools');

    expect(TestBed.inject(Title).getTitle()).toBe('Werkzeuge · Schaffbar');
  });

  it('should show only the app name for a route without title', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/untitled');

    expect(TestBed.inject(Title).getTitle()).toBe('Schaffbar');
  });
});
