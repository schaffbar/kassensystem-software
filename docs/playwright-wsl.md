# Playwright für Claude Code (WSL)

Damit Claude Code das Portal im Browser öffnen, durchklicken und Screenshots ansehen kann, läuft Chromium in WSL und
ist über den Playwright-MCP-Server angebunden. Alle Befehle im **WSL-Terminal**, nicht in Windows.

Der MCP-Server bringt sein eigenes Playwright mit, und jede Playwright-Version erwartet genau eine Chromium-Revision
(`@playwright/mcp@0.0.84` → `chromium-1252`). Deshalb wird die Version **festgelegt** und Chromium **über das Paket des
MCP-Servers** installiert – nicht mit einem allgemeinen `npx playwright install`, das eine andere Revision lädt.

## 1. Claude Code CLI in WSL

Die VS-Code-Erweiterung bringt ihre eigene Claude-Version mit, der Befehl `claude` fehlt in WSL aber zunächst:

```bash
npm install -g @anthropic-ai/claude-code
claude --version
```

## 2. Chromium installieren

```bash
# Linux-Bibliotheken, die Chromium zum Starten braucht (libnss3, libgbm, Schriften, …) – einmalig
sudo env "PATH=$PATH" npx playwright install-deps chromium

# Chromium in der Revision, die der MCP-Server erwartet (nach ~/.cache/ms-playwright, ohne sudo)
npx -y -p @playwright/mcp@0.0.84 playwright install chromium
```

`sudo env "PATH=$PATH"` ist nötig, wenn Node über nvm installiert ist: `sudo` hat einen eigenen `PATH` ohne den
nvm-Ordner und meldet sonst `sudo: npx: command not found`. Alternative ohne npx als root:

```bash
npx playwright install-deps --dry-run chromium   # zeigt nur den apt-get-Befehl
sudo apt-get install …                           # den angezeigten Befehl selbst ausführen
```

## 3. MCP-Server eintragen

Im Wurzelordner des Projekts (dort, wo VS Code geöffnet ist):

```bash
cd ~/source/schaffbar/kassensystem-software
claude mcp add playwright -- npx @playwright/mcp@0.0.84 --browser chromium --headless --isolated
```

- `--browser chromium` – ohne die Option sucht der Server Google Chrome unter `/opt/google/chrome/chrome`
  (Fehler „Chromium distribution 'chrome' is not found“).
- `--headless` – kein Fenster, unter WSL am zuverlässigsten.
- `--isolated` – jede Sitzung startet ohne alte Cookies und alten Speicher.
- Der Eintrag gilt nur für dich und diesen Ordner (`~/.claude.json`), nicht im Repo.

## 4. Prüfen

1. Claude Code neu starten (Sitzung bzw. Claude-Panel in VS Code schließen und neu öffnen) – MCP-Server werden nur
   beim Start geladen.
2. `/mcp` in Claude Code oder `claude mcp list` im Terminal zeigt `playwright: … ✔ Connected`.
3. Claude bitten, `http://localhost:4200` zu öffnen und einen Screenshot zu machen.

Ohne MCP geht ein einzelner Screenshot auch direkt:

```bash
npx -y -p @playwright/mcp@0.0.84 playwright screenshot --browser chromium --viewport-size "1440,900" \
  --wait-for-timeout 4000 http://localhost:4200/ screenshot.png
```

## Aktualisieren

Neue MCP-Version: Versionsnummer in Schritt 2 und 3 ersetzen, beide Befehle erneut ausführen
(`claude mcp remove playwright`, dann `claude mcp add …`). Alte Revisionen in `~/.cache/ms-playwright` können
gelöscht werden.

## Für sinnvolle Screenshots

- `ng serve` im Ordner `schaffbar-portal` (startet Claude auch selbst).
- Backend unter `http://127.0.0.1:5000` (`environment.ts`) mit ein paar Testdaten – Benutzer, Werkzeuge,
  Lesegeräte, Tags. Ohne Backend sind die Seiten leer bzw. zeigen Ladefehler.
