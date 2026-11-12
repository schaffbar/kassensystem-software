---
name: verify-ui
description: How to check a change of schaffbar-portal in the real browser without touching the database – start the dev server, drive it with the Playwright MCP server, block every write request to the backend, simulate states with intercepted responses (open session, waiting RFID tag, tool in use, new reader), take screenshots on desktop and phone, and check for horizontal scrolling and misplaced elements. Use after changing a page, component, style or dialog of the portal, when asked to look at, screenshot or click through the portal, or when a UI change must be verified before handing it over.
---

# Verifying the portal in the browser

Tests prove the logic; only the browser shows whether a page looks and behaves right – content projection, wrapping,
an element that lands in the wrong place, a dialog that is cut off. Every UI change is looked at on desktop and phone
before it is handed over.

**The database is the real one** (the backend at `http://127.0.0.1:5000/api/v1`, `environment.apiBaseUrl`). Nothing
may be changed there: every request except `GET` is intercepted – aborted, or answered by the test itself.

Setup of Playwright under WSL: [`docs/playwright-wsl.md`](../../../../docs/playwright-wsl.md). The tools are those of the
MCP server `playwright` (`mcp__playwright__browser_*`); most work goes through `browser_run_code_unsafe`, which gets
the Playwright `page`.

## 1. Dev server

```bash
# in the background, output into the scratchpad (not into the repo)
npx ng serve --port 4200 > <scratchpad>/ng-serve.log 2>&1
```

Start it with `run_in_background`; it rebuilds on every save. Do not start a second one – check first whether it
still runs (a background shell of the session, or `curl -s localhost:4200`). The backend is not started by the
agent; if it does not answer, say so and stop.

Wait until it is ready inside the first Playwright call:

```js
for (let i = 0; i < 30; i++) {
  try { if ((await page.request.get('http://localhost:4200/')).ok()) break; } catch {}
  await page.waitForTimeout(1000);
}
```

After a save, give the rebuild a moment (`page.waitForTimeout(3000)` or reload until the change is there).

## 2. Block writes – always first

```js
await page.unrouteAll({ behavior: 'ignoreErrors' });
await page.route('http://127.0.0.1:5000/**', (route) =>
  route.request().method() === 'GET' ? route.continue() : route.abort(),
);
```

Set it up in every `browser_run_code_unsafe` call that navigates (`unrouteAll` first, so handlers don't pile up).
Clicking "Speichern", "Löschen" or "Zuweisen" then fails like a server error – which is fine for looking at the
dialog. To see the flow after a write, answer it in the route instead (see "Simulating a flow").

## 3. Simulating states

Real data rarely has the state you need. Answer single requests with your own JSON and let everything else through:

```js
const id = 'e5d24bd1-…';   // a real id, found with a GET: (await page.request.get(`${api}/customers`)).json()
const today = (h, m) => { const d = new Date(); d.setHours(h, m, 0, 0); return d.toISOString(); };

await page.route('http://127.0.0.1:5000/**', async (route) => {
  const req = route.request();
  if (req.method() !== 'GET') return route.abort();
  const url = req.url();
  if (url.includes(`/workshop-sessions/${id}/active`)) return route.fulfill({ json: session });
  if (url.includes('/rfid-tag-assignments') && url.includes(id)) return route.fulfill({ json: [waitingAssignment] });
  return route.continue();
});
```

- **The JSON must pass the zod schema** of the endpoint (`src/app/shared/api/<resource>/*.api-model.ts`) – every
  field, `null` where nullable, ISO strings for dates. A wrong shape shows "Die Daten konnten nicht geladen werden"
  and logs the zod path to the console (`browser_console_messages`).
- **Change a real list** instead of writing it: `const json = await (await route.fetch()).json(); json.push(…);
  return route.fulfill({ json });` (a new reader without type in the real reader list).
- Times relative to today (`today(14, 5)`) – a fixed date shows the date (`timeWithDay`) and "not checked out".
- States worth checking per page: empty list, one entry, many entries (wrapping, "and N more"), a missing value
  (dash), a warning (reader without type), a blocked action (danger zone with explanation, disabled button with
  hint), loading error (respond with `route.fulfill({ status: 500 })`).

## 4. Simulating a flow

For a dialog that waits for the backend (request a tag, then the tag is held to the reader), answer the write and
change what the next `GET` returns:

```js
let assignment = [];
await page.route('http://127.0.0.1:5000/**', async (route) => {
  const req = route.request();
  if (req.method() === 'POST' && req.url().endsWith('/rfid-tag-assignments')) {
    assignment = [{ …, status: 'WAITING_FOR_ASSIGNMENT', rfidTagId: null }];
    return route.fulfill({ status: 201, body: '' });           // answered here – nothing reaches the backend
  }
  if (req.method() !== 'GET') return route.abort();
  if (req.url().includes('/rfid-tag-assignments') && req.url().includes(id)) return route.fulfill({ json: assignment });
  return route.continue();
});
// … click "Weiter – Tag auflegen", screenshot the waiting step …
assignment = [{ …, status: 'ASSIGNED', rfidTagId: '04C95A12' }];   // the tag arrives
await page.waitForTimeout(2600);                                    // the next poll
```

## 5. Looking at it

Two sizes, always:

```js
const dir = '<scratchpad>/';
await page.setViewportSize({ width: 1440, height: 1000 });         // desktop
await page.goto('http://localhost:4200/users/' + id);
await page.waitForSelector('schbar-user-session-banner');           // what the page must show, not a fixed wait
await page.waitForTimeout(500);                                     // fonts and animations
await page.screenshot({ path: dir + 'user-desktop.png', fullPage: true });

await page.setViewportSize({ width: 390, height: 844 });            // phone
await page.waitForTimeout(500);
const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
await page.screenshot({ path: dir + 'user-phone.png', fullPage: true });
await page.setViewportSize({ width: 1440, height: 1000 });          // back for the next call
return { overflow };
```

Then read every screenshot (`Read` on the png) – and actually look: alignment, wrapping, cut-off badges, empty space,
the order of elements. Screenshots go into the scratchpad, never into the repo.

- **A detail:** `clip` on the area (`const box = await page.locator('schbar-…').boundingBox()`), sharper than a full
  page.
- **Measure instead of guessing:** `boundingBox()` for widths ("are the two cards half and half?"), the visible height
  of a dialog against its content (`scrollHeight` vs. `clientHeight` of `mat-dialog-content`) for "does it scroll?".
- **Menus and dialogs:** open them by role (`page.getByRole('button', { name: 'Aktionen für Hans Schmidt' })`,
  `getByRole('menuitem', …)`), read the entries with `page.locator('.mat-mdc-menu-item').allTextContents()`, close with
  `Escape` or "Abbrechen".
- **Tabs and deep links:** `?tab=open-session`, then check `[role="tab"][aria-selected="true"]`.
- **Console:** `browser_console_messages` after loading – a zod error or an Angular error is a finding.

## What to look for

- Header meta (badges) next to the title, not among the actions – content projection into nested control flow
  (skill `design`).
- Nothing scrolls sideways on the phone except a table inside `table-scroll`; badges not cut off; columns wrap.
- A disabled button explains why next to it; a dialog fits without scrolling where it should.
- Texts are German and translated (no `users.pages.…` keys on screen).
- The progress bar does not flicker on a page that reloads itself.

## Report

Say what was looked at (pages, states, sizes), what was simulated, and what was found and fixed. Name the screenshot
paths only if the user should open them. Never claim a state was checked that was not.
