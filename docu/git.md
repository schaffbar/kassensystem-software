# Git

## Teile vom branch zu neuen repository verschieben

### Phase 1: Temporären Clone vorbereiten

`kassensystem` in ein temporäres Verzeichnis klonen (Branch `piotr`):

```bash
git clone --branch piotr /Users/krantzp/source/private/kassensystem /tmp/kassensystem-migrate
```

`git-filter-repo` installieren (falls nicht vorhanden):

```bash
brew install git-filter-repo
```

### Phase 2: History filtern

Nur die gewünschten Pfade behalten — alle anderen Commits/Dateien werden entfernt:

```bash
cd /tmp/kassensystem-migrate
git filter-repo \
  --path schaffbar-backend/ \
  --path schaffbar-portal/ \
  --path deployment.md \
  --path interfaces.md \
  --path use-cases.md \
  --path workshop-usage.md
```

Commits, die _nur_ Hardware-Dateien berührt haben, werden automatisch gedroppt.

### Phase 3: In kassensystem-software mergen

Den gefilterten Clone als Remote hinzufügen:

```bash
cd /Users/krantzp/source/private/kassensystem-software
git remote add migration /tmp/kassensystem-migrate
git fetch migration
```

Merge mit `--allow-unrelated-histories` (da unterschiedliche Repo-Wurzeln):

```bash
git merge migration/piotr --allow-unrelated-histories
```

Temporären Remote entfernen:

```bash
git remote remove migration
```

### Phase 4: Aufräumen

Temporären Clone löschen: `rm -rf /tmp/kassensystem-migrate`
Push: `git push origin main`

### Verification

1. Nach filter-repo: `git log --oneline` im Temp-Clone → nur relevante Commits vorhanden
2. Nach Merge: `git log --oneline` in kassensystem-software → History ist vollständig
3. `ls` zeigt schaffbar-backend, schaffbar-portal und die Doku-Dateien
4. Keine Hardware-Dateien vorhanden: `find . -path './platine*' -o -path './ArduinoIDE*'` → leer

### Decisions

- timeMachine wird **nicht** migriert
- Commits die ausschließlich ausgeschlossene Dateien betrafen, fallen weg (gewollt)
- Original-Repo `kassensystem` bleibt vollständig unverändert
- Verzeichnisstruktur bleibt identisch
