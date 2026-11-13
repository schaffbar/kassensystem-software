---
name: design
description: The design system of schaffbar-portal (Angular 22, Angular Material M3) and how pages are laid out – color tokens, the shared building blocks (page header, status badge, row menu, filter chips, empty state, danger zone, …), global layout classes, how a list page, a detail page and the overview are built, status vs. text vs. dash, buttons, phone layout, and the workflow for building a page from the prototype. Use when building or changing the look of a page, card, list, table, badge or button, choosing a building block, styling a component, converting a page to the prototype, or reviewing the UI.
---

# Design system and page layout in schaffbar-portal

The design comes from the prototype (artifact <https://claude.ai/artifact/MUj2FTVMMR2MEUWdbJ5vEM>, one artboard
`project/<Page>.dc.html` per page and dialog) and is described in [`docs/ui-redesign.md`](../../../../docs/ui-redesign.md)
(colors, type, UX principles, every decision with its date). This skill is the Angular side: which token, which
building block, which layout.

Reference pages:

- list: [user-list/](../../../src/app/users/pages/user-list/), [rfid-reader-list/](../../../src/app/rfid-readers/pages/rfid-reader-list/)
- detail with tabs and two columns: [user-detail/](../../../src/app/users/pages/user-detail/),
  [tool-detail/](../../../src/app/tools/pages/tool-detail/)
- detail without tabs, one column: [rfid-reader-detail/](../../../src/app/rfid-readers/pages/rfid-reader-detail/)
- overview: [dashboard/](../../../src/app/dashboard/pages/dashboard/)

## Building a page from the prototype

1. **Read the artboard first** – the page's `.dc.html` and the artboards of its dialogs, not only the text in
   `ui-redesign.md`. Take sizes, colors, texts and order from it; never copy its inline-style HTML.
2. Map every part to a building block or global class below. A part without one: a dumb component in the feature
   (skill `component`), styled with tokens.
3. **Check the data.** What the backend does not deliver yet is left out, not faked. Collect the gaps and ask before
   building when the prototype contradicts the backend (e.g. "close the session" while the backend forbids it).
4. Record every decision with its date in the page's entry in `docs/ui-redesign.md` ("Phase 1"), backend gaps in
   [`docs/backend-features.md`](../../../../docs/backend-features.md), rules the portal checks for the backend in the
   aggregate's `README.md` ("Enforced only in the portal").
5. Look at it in the browser on desktop and phone (skill `verify-ui`).

## Tokens

Defined on `:root` in [`src/styles.scss`](../../../src/styles.scss) with `light-dark()` – dark mode needs only
`color-scheme: dark`. **Component styles use tokens only, never hex values.**

| Token | Use |
| --- | --- |
| `--bg`, `--surface`, `--surface-2` | page background, cards, subtle fills (avatars, counts, hover) |
| `--border` | every line and card border |
| `--text`, `--text-2`, `--text-3` | main text, secondary text (values, details), tertiary (labels, meta, times) |
| `--accent` | neon green **only as a surface** (main button, active bar, pulse dot) – as text it has 2:1 |
| `--accent-text` | green text and links |
| `--info`, `--warn`, `--danger` (+ `-soft`) | status colors: text/icon on the `-soft` background |
| `--on-accent`, `--on-danger` | text on a filled accent / danger button |
| `--nav`, `--nav-2`, `--nav-text*` | the navy of the sidebar – also icon tiles, avatars, the session button |
| `--focus` | focus outline (`outline: 2px solid var(--focus)`) |
| `--font-mono` | via the class `mono` |

A softer shade of a token: `color-mix(in srgb, var(--accent) 18%, transparent)`, not an `rgb()` value.

Radii: 8 icon button, 10 button / input / small tile, 12–14 card, 16 dialog, `999px` badge and chip.

## Material

- The look of Material components comes from `mat.*-overrides(...)` – globally in `styles.scss`, locally in a
  component's scss for one special button (`.close-button` in `user-session-banner`: navy filled button). Never
  `.mat-mdc-*` selectors or `!important`.
- Buttons: `matButton="filled"` main action, `matButton="outlined"` secondary, `matButton` (text) for a light action
  in a card header ("Bearbeiten" in a card), `matIconButton` with a translated `aria-label` – the attribute syntax,
  not `mat-flat-button` / `mat-stroked-button` / `mat-icon-button`. No `color="…"`: it has no effect in the M3 theme.
- Destructive: global class `danger-button` (any button variant) and `danger-menu-item` (menu entry) – both always
  ask first (skill `dialog`).

## Building blocks (`src/app/shared/components/`)

Their texts are passed **translated**.

| Block | Use |
| --- | --- |
| `schbar-page-header` | every page: `title`, `subtitle`, `breadcrumbs` (detail pages), `icon` (navy tile) or `avatar` (initials of a person); actions projected; badges and meta with `pageHeaderMeta` |
| `schbar-status-badge` | a status as icon + text (`tone`: `success`, `info`, `warn`, `danger`, `neutral`); without icon only `neutral` for a plain property; not inside a `mat-option` – `mat-select` takes the icon name into the text |
| `schbar-certification-status-badge`, `schbar-certification-requirement-badge` | an enum mapped once to tone, icon and text – give a new enum its own badge like these |
| `schbar-row-menu` | the ⋯ menu of a row or a header; destructive entries last after `<mat-divider />` |
| `schbar-filter-chips` | the filters of a list with counts (`[(selected)]`); counts ignore the search |
| `schbar-toggle-chip` | one on/off filter (`[(pressed)]`) next to the chips |
| `schbar-segmented-control` | one choice out of a few (area, cards / list) with optional counts; inside a card for a local filter (tool status of the overview) |
| `schbar-empty-state` | instead of an empty list or table (`icon`, `title`, `message`); also for "no results" of a search |
| `schbar-danger-zone` | last card of a detail page (see below) |
| `schbar-field-error` | the error text of a form field (skill `dialog`) |

Pipes (`src/app/shared/pipes/`): `orDash` (missing value), `timeWithDay` (time of a session: `17:50` today,
`10.11.26, 17:50` otherwise), `rfidTagId` (`04A3 1F9C`, with class `mono`), `ageGroupKey`, `toolAreaKey`,
`certificationRequirementKey` (enum → translation key).

## Global classes (`src/styles.scss`)

| Class | Use |
| --- | --- |
| `card` + `card-header` | a section of a detail page; `h2` left, actions right; a dumb component that is one card: `host: { class: 'card' }` |
| `table-card` + `table-scroll` | frame of a list table (and paginator); the table scrolls inside on narrow screens; also a card whose rows run to the edge (certifications, people in the workshop) – then the host needs `display: block` |
| `clickable-row` | table row that opens the detail page; its ⋯ menu offers "Details" too (a row can't be reached by keyboard) |
| `detail-list` | `<dl>` with the read-only values of a card |
| `search-field` | the search `mat-form-field` next to the filter chips |
| `chip`, `chip-count` | look of the filter chips (used by the blocks) |
| `radio-card`, `radio-card-text`, `radio-card-label`, `radio-card-description` | a choice with an explanation instead of a `mat-select` |
| `mono`, `mono-field` | IDs, MAC / IP addresses, tag IDs |
| `form-dialog`, `compact-form`, `field-label`, `form-row`, `form-col`, `form-actions`, `full-width`, `readonly-field` | forms (skill `dialog`) |

## Status, text or dash

| Situation | Show |
| --- | --- |
| a state the user should notice (active, paused, in use, waiting) | `schbar-status-badge` with icon – never color alone |
| a plain property (type of a reader, age group) | neutral badge without icon, or plain text in the meta line |
| a value that does not exist (yet) – end time of a running usage, no phone | `orDash` (`–`) |
| a missing value someone has to act on – reader without type, not checked out since yesterday | translated text with warning icon in `--warn` / `--danger`, and a hint where it can be fixed |
| something normal that is just absent – a user without tag at home | dash or nothing, not a warning |
| a running thing | "läuft" in `--info`, badge `tone="info"` icon `play_circle` |

## List page

Header with count in the subtitle and the main action ("Benutzer hinzufügen") → `schbar-filter-chips` with the
search next to it → `table-card` with `table-scroll` and `mat-table` (or cards, `schbar-segmented-control` to switch)
→ `schbar-empty-state` instead of an empty table, a second one for "no results". Rows are `clickable-row`, the last
column a `schbar-row-menu` ("Details" first, destructive entries last). Sorted by what the user looks for (name),
never by a value the list does not show.

## Detail page

```
schbar-page-header   breadcrumbs · icon/avatar · title · meta badges (pageHeaderMeta) · Bearbeiten · reload · ⋯
[banner]             one flat line for a state that needs action now (open session) – optional
mat-tab-group        mat-stretch-tabs="false" mat-align-tabs="start", count in the label (.tab-count)
  first tab          .columns: main column (flex 2) + side column (flex 1), wrapping to one column
                     schbar-danger-zone – last, over the full width below both columns
```

- **Meta in the header:** one element per `@if` – content projection with `pageHeaderMeta` does not reach into a
  nested `@switch` (the badge lands among the actions). Compute the case in the class or use `@if / @else if`.
- **Danger zone:** every detail page that can delete its entity has one, the ⋯ menu of the header offers the same.
  When a rule forbids deleting (open session, tool in use, reader assigned to a tool) the card explains why – with a
  link to where it can be resolved – instead of the button, and the menu hides the entry.
- **Editing:** values are read-only in a card (`detail-list` or a list with icons); "Bearbeiten" opens a form dialog.
- **Dialogs opened from several places** (header, menu, banner, tabs): a page service (skill `dialog`).
- **Live data:** a page that stays open (overview, a waiting RFID tag) reloads with `rxMethod` + `interval`, and its
  `isLoading` only counts the first load, so the progress bar does not flicker (skill `feature`).

## Overview

Header with date and "aktualisiert um" → "Braucht Aufmerksamkeit" (warn-soft block, only when there is something,
one line per kind with a link to where it is fixed) → key figures (`stat-tile` of the dashboard, grid
`minmax(150px, 1fr)`) → two cards side by side, half and half (`flex: 1 1 400px`). Rows that open something are `<a>`
with `routerLink`, not click handlers.

## Rows inside a card

`<ul>` without bullets, each `<li>` with `border-top: 1px solid var(--border)`, min height ~58–60px, padding
`10px 20px`; text block `flex: 1 1 auto; min-width: 0` (name in `font-weight: 600`, detail in `--text-3` 13px);
badges below the name, not between name and time, so they never squeeze it; time / numbers right with
`font-variant-numeric: tabular-nums`. A whole row as link: `<a>` with `display: flex`, hover `--surface-2`,
`:focus-visible` outline inside.

## Phone (< 600px)

- Columns (`flex-wrap` with a `flex-basis`) fall to one column by themselves; key figures stay two side by side.
- Nothing scrolls sideways except a `table-scroll`. Long values wrap (`overflow-wrap: anywhere`), a time column may
  wrap, badges do not get cut.
- Cards use `padding: 16px` (global `card` does it).

## Checklist

- [ ] Artboard read; every deviation and backend gap asked, decided and recorded with date
- [ ] Only tokens, no hex values; neon green only as surface
- [ ] Building blocks and global classes instead of own copies
- [ ] Status = icon + text; missing value = dash or a highlighted hint, as above
- [ ] Every `matIconButton` has a translated `aria-label`; destructive actions ask first
- [ ] Danger zone last on the first tab, over the full width
- [ ] Desktop and phone looked at in the browser (skill `verify-ui`)
