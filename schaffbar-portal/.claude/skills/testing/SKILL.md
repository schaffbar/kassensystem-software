---
name: testing
description: How to write and run the unit tests of schaffbar-portal (Angular 22, Vitest, zoneless) – store specs with HttpTestingController (trigger/settle, flushing every resource, mutations, polling), component and page specs (ViewModel in, command out, store mocks with signals, child() helper), Material harnesses for forms, menus in the overlay, dialogs, fake timers, the German locale for times, translations in tests, and the pitfalls that make tests hang or flicker. Use when writing, fixing or reviewing a *.spec.ts, when a test hangs, times out or fails only in the full run, or when testing a store, page, component, dialog, pipe or mapper.
---

# Tests in schaffbar-portal

Vitest through the Angular builder (`@angular/build:unit-test`), zoneless, no global setup file – every spec imports
from `vitest` itself. What to test where (store vs. page vs. dumb component) is in skill `component` ("Testing") and
skill `dialog`; this skill is the how.

```bash
npm run check                                                      # everything before handing over: lint, prettier,
                                                                   # spec types, all tests, build
npx ng test --watch=false                                          # all tests
npx ng test --watch=false --include 'src/app/users/**/*.spec.ts'   # a folder – always end the glob in .spec.ts,
                                                                   # 'src/app/users/**' also picks up html/scss
npx tsc -p tsconfig.spec.json --noEmit                             # type check of the specs (ng test reports less)
```

A failure that makes no sense right after an edit: run again – the test build sometimes still has the old code.

Reference specs:

- store with resources, mutations and polling: [user-detail.store.spec.ts](../../../src/app/users/pages/user-detail/user-detail.store.spec.ts)
- page with tabs, header menu and banner: [user-detail.component.spec.ts](../../../src/app/users/pages/user-detail/user-detail.component.spec.ts)
- tab component: [user-detail-tab-overview.component.spec.ts](../../../src/app/users/pages/user-detail/user-detail-tab-overview/user-detail-tab-overview.component.spec.ts)
- form dialog with harnesses: [update-user-form.component.spec.ts](../../../src/app/users/components/update-user-form/update-user-form.component.spec.ts)
- dialog fed by bindings: [assign-rfid-tag-dialog.component.spec.ts](../../../src/app/users/components/assign-rfid-tag-dialog/assign-rfid-tag-dialog.component.spec.ts)
- row menu in the overlay: [user-tool-certifications.component.spec.ts](../../../src/app/users/components/user-tool-certifications/user-tool-certifications.component.spec.ts)
- page service opening dialogs: [user-detail-dialogs.service.spec.ts](../../../src/app/users/pages/user-detail/user-detail-dialogs.service.spec.ts)

## Basics

```ts
import { provideZonelessChangeDetection } from '@angular/core';
import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

TestBed.configureTestingModule({
  imports: [XComponent],
  providers: [provideZonelessChangeDetection(), provideTranslateService(), provideRouter([]) /* if it links */],
});
```

- **Texts are translation keys.** `provideTranslateService()` without translations renders the key, without its
  parameters: expect `'users.pages.userDetails.registeredOn'`, not the German text. Where a parameter is the point of
  the test (a list of names, a count), load a translation in the spec:
  `TestBed.inject(TranslateService).setTranslation('de', { … }); TestBed.inject(TranslateService).use('de');`
  ([dashboard-attention spec](../../../src/app/dashboard/components/dashboard-attention/dashboard-attention.component.spec.ts)).
- **Text content** of an element: normalize whitespace – `textContent.replace(/\s+/g, ' ').trim()`. A Material icon is
  text too: a status badge reads `'play_circle dashboard.toolStatus.statuses.inUse'`, a menu item
  `'deleteusers.menu.delete'` (no space). Adjacent elements without whitespace between them join
  (`'Bandsägeshared.toolAreas.HOLZ'`) – expect what the DOM gives, don't add spaces to the template for a test.
- Typed mocks: `WritableSignal<T>` for store signals (not `ReturnType<typeof signal<T>>`), `Mock` for functions.

## Test data

- Feature models and ViewModels as **complete typed literals** (`const user: User = { … }`), no `as` casts, no
  fields the component doesn't read.
- API data from the fixtures next to the API model, with overrides: `customerFixture({ ageGroup: AgeGroup.Under18 })`,
  `toolFixture(…)`, `workshopSessionFixture(…)` (`src/app/shared/api/**/*.api-fixtures.ts`). A new API model gets a
  fixture there.
- **Dates local**: `new Date(2026, 10, 9, 14, 5)` (month 0-based), not ISO strings, unless the test is about parsing.
  A small helper per spec keeps them readable: `const at = (hours: number, minutes: number) => new Date(2026, 10, 9, hours, minutes);`
- Code that compares with `new Date()` (today, overnight, durations): either fake the clock (see "Timers") or build
  the data relative to now – `startOfDay(new Date())`, `subHours(startOfDay(today), 4)` – never a fixed date that is
  "today" only on the day the test was written.

## Store specs

The store runs against `HttpTestingController`; nothing is mocked except HTTP (and the router if it navigates).

```ts
TestBed.configureTestingModule({
  providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), provideRouter([]), XStore],
});

// lets the resources react to signal changes and send their requests
function trigger(): void {
  TestBed.tick();
}

// processes the flushed responses
async function settle(): Promise<void> {
  await TestBed.inject(ApplicationRef).whenStable();
}

const expectGet = (url: string, param?: [string, string]): TestRequest =>
  httpMock.expectOne((req) => req.method === 'GET' && req.url === url && (!param || req.params.get(param[0]) === param[1]));

async function loadUser(): Promise<void> {
  store.setUserId('user-1');
  trigger();
  expectUser().flush(user);
  expectAssignment().flush([assignment]);
  // … every resource of the store, also those without an id (they load when the store is created)
  await settle();
}

afterEach(() => httpMock.verify());
```

- **`settle()` only when no request is open.** `whenStable()` waits for pending resources – with an unflushed request
  it hangs until the test times out. Order: trigger → flush all → settle. After a mutation: `flush` the command →
  `trigger()` → flush the reload → `settle()`.
- Flush **every** resource the store creates, also the ones loaded without an id (`toolsResource()`,
  `customersResource()`); `httpMock.verify()` names the forgotten one.
- A command: `httpMock.expectOne((req) => req.method === 'PUT' && req.url === …)`, check `request.body`, flush `null`,
  then expect the reload of exactly the affected resource.
- Several requests one after another (`concat`): the next one exists only after the previous was flushed –
  `httpMock.expectNone(url)` before, `expectOne` after.
- Navigation in `onSuccess`: `vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)`.
- A response that breaks the zod schema: `vi.spyOn(console, 'error').mockImplementation(() => undefined)` (parseWith
  logs), then expect `hasLoadError()` and the empty data; `vi.restoreAllMocks()` in `afterEach`.
- A method created with `rxMethod` that takes a signal (`pollRfidTagAssignment(isWaiting)`) needs an injection
  context in the spec: `TestBed.runInInjectionContext(() => store.poll…(store.isWaiting…))`, then `trigger()`.
- A one-off check (`canDeleteUser(): Observable<boolean>`): subscribe, flush the request, expect the value.

## Timers

Fake only what the test needs – `whenStable()` and Angular use real timeouts:

```ts
// polling: only the interval
vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
TestBed.runInInjectionContext(() => store.pollDashboard(60_000));
trigger();
vi.advanceTimersToNextTimer();   // the next tick – not advanceTimersByTime(60_000), the exact ms are not reliable
trigger();
// … expect and flush the reload; when the poll should stop: expect(vi.getTimerCount()).toBe(0)

// "now" for durations or "today": only the clock
vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 10, 9, 22, 0) });

afterEach(() => vi.useRealTimers());
```

A component that ticks with `toSignal(interval(…))` keeps its real interval when only `Date` is faked.

## Times and dates in the DOM

The tests run in `en-US` – `14:05` would be `2:05 PM`. A spec that checks a formatted time or date sets the locale of
the app:

```ts
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';

registerLocaleData(localeDe);
// providers:
{ provide: LOCALE_ID, useValue: 'de-DE' },
```

`timeWithDay` shows the date for a time that is not today – fake `Date` to the day of the test data, or expect the
date (`'08.11.26, 19:49'`).

## Components

Dumb component – ViewModel in, command out:

```ts
fixture = TestBed.createComponent(XComponent);
fixture.componentRef.setInput('userRfidTagCard', viewModel);
await fixture.whenStable();

const rfidTagUnassigned = vi.fn();
fixture.componentInstance.rfidTagUnassigned.subscribe(rfidTagUnassigned);
// click …
expect(rfidTagUnassigned).toHaveBeenCalledExactlyOnceWith({ userId: 'user-1' });
```

- Confirmation services mocked: `{ provide: ConfirmActionDialogService, useValue: { open: vi.fn(() => of(true)) } }`;
  test confirmed and not confirmed.
- A dumb component with `RouterLink` needs `provideRouter([])`; expect the link as `getAttribute('href')`
  (`'/users/user-1?tab=open-session'`).

Page or tab – store mocked with signals, children real:

```ts
storeMock = {
  user: signal(user),                                             // WritableSignal – set() in a test changes the page
  updateUser: vi.fn(),
  assignRfidTag: Object.assign(vi.fn(), { isPending: signal(false) }),   // a mutation whose isPending is read
};
await TestBed.configureTestingModule({ imports: [UserDetailComponent], providers: [...] })
  // a page provides its store itself – a TestBed provider doesn't reach it
  .overrideComponent(UserDetailComponent, { set: { providers: [{ provide: UserDetailStore, useValue: storeMock }] } })
  .compileComponents();
```

- A tab component has no providers of its own: the mock goes into the TestBed providers (store and page service).
- The mapping of the page is tested through the child: `child(UserToolCertificationsComponent).certifications()` with

  ```ts
  function child<T>(type: new (...args: never[]) => T): T {
    return fixture.debugElement.query(By.directive(type)).componentInstance;
  }
  ```

  and outputs by emitting on the child: `child(UserRfidTagCardComponent).rfidTagUnassigned.emit(command)` → expect
  the store method.
- Every signal the template reads must be in the mock (also those of the tabs, which share the page's store) – a
  missing one fails with "… is not a function".
- Changing a mock signal: `storeMock.openSession.set(session); await fixture.whenStable();`.
- Route inputs: `fixture.componentRef.setInput('id', 'user-1')` before the first `whenStable()`.

## Menus and other overlays

`schbar-row-menu`, `mat-select` and dialogs render into `.cdk-overlay-container` in the document, not into the
fixture:

```ts
afterEach(() => document.querySelector('.cdk-overlay-container')?.replaceChildren());

async function openMenu(): Promise<string[]> {
  fixture.nativeElement.querySelector('schbar-row-menu button').click();
  await fixture.whenStable();
  return [...document.querySelectorAll('.mat-mdc-menu-item')].map((item) => item.textContent!.trim());
}
```

Click entries with `document.querySelectorAll<HTMLElement>('.mat-mdc-menu-item')[i].click()`; a hidden entry is
checked by its absence in the list.

## Forms and harnesses

```ts
loader = TestbedHarnessEnvironment.loader(fixture);

const input = (id: string) => loader.getHarness(MatInputHarness.with({ selector: `#${id}` }));
await (await input('firstName')).setValue('Moritz');
await (await loader.getHarness(MatButtonHarness.with({ text: 'shared.actions.saveChanges' }))).click();
```

- Fields with the label above (`field-label`) have no floating label – find the field by its input id:

  ```ts
  async function errors(id: string): Promise<string[]> {
    for (const field of await loader.getAllHarnesses(MatFormFieldHarness)) {
      const control = await field.getControl();
      if (control && (await (await control.host()).getAttribute('id')) === id) return field.getTextErrors();
    }
    throw new Error(`no field #${id}`);
  }
  ```

- A button with an icon has the icon name in its text – match with a regex:
  `MatButtonHarness.with({ text: /shared.dialogs.closeSession.actions.confirm/ })`.
- Datepicker: `provideDateAdapter()` in the providers, `MatDatepickerInputHarness.setValue('15.3.1985')`.
- Radio cards: `MatRadioGroupHarness` – `getCheckedValue()`, `checkRadioButton({ label: /…/ })`.
- Checkbox: `MatCheckboxHarness.check()`.

## Dialogs

- Form dialog: `{ provide: MAT_DIALOG_DATA, useValue: data }`, `{ provide: MatDialogRef, useValue: { close: vi.fn() } }`;
  expect `close` with the command, `close(undefined)` on cancel.
- Anything with `schbar-form-dialog-header` (its close button) or `[mat-dialog-close]` needs the `MatDialogRef` mock,
  also a modal of a shared dialog.
- A dialog fed by `inputBinding` / `outputBinding`: create it like a component, `setInput` the bound inputs, subscribe
  to the outputs.
- A dialog service: real `MatDialog`, check `openDialogs[0].componentInstance` and close it
  (`openDialogs[0].close(true)`); `TestBed.inject(MatDialog).closeAll()` in `afterEach`.
- A page or page service that opens dialogs: mock `MatDialog` (`{ open: vi.fn() }` returning
  `{ afterClosed: () => of(command) }`) and the dialog services.

## Pitfalls

| Symptom | Cause |
| --- | --- |
| test times out after 5 s | `settle()` / `whenStable()` with an unflushed request; or fake timers for `setTimeout` |
| "Expected no open requests, found 1" | a resource of the store not flushed (often one without id) |
| "Cannot configure the test module when the test module has already been instantiated" | a previous test of the file failed mid-way – fix the first error |
| `2:05 PM` instead of `14:05` | `LOCALE_ID` `de-DE` missing |
| a date where only a time was expected | `timeWithDay` and test data of another day – fake `Date` |
| badge or meta in the wrong place of the header | content projection into nested `@switch` (skill `design`) – a real bug, not a test problem |
| a `computed` based on `linkedSignal` does not update | the linked signal is only evaluated when read – read it on every evaluation or derive without it |

## Checklist

- [ ] Spec imports from `vitest`, `provideZonelessChangeDetection()`, translation keys expected
- [ ] Store: every resource flushed, `httpMock.verify()`, reload after each mutation checked
- [ ] Dates local or relative to now; locale `de-DE` where times are checked
- [ ] Fake timers only for what is needed, `vi.useRealTimers()` in `afterEach`
- [ ] Overlays cleaned up in `afterEach`
- [ ] Both outcomes of a confirmation tested
- [ ] Runs alone and in the full run (`npx ng test --watch=false`), `tsc -p tsconfig.spec.json` clean
