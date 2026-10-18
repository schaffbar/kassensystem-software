# Setup

## Arduino IDE

Diese Anleitung beschreibt Schritt für Schritt, wie man die rfidReader-Software mit der
Arduino IDE (Version 2.x) übersetzt und auf den ESP32 flasht.

### 1. Repository holen

```bash
git clone https://github.com/schaffbar/kassensystem.git
```

Die relevante Ordnerstruktur:

```bash
kassensystem/
├── ArduinoIDE_libraries/        <- alle benötigten Bibliotheken (im Repo enthalten)
└── rfidReader_ArduinoIDE/
    └── rfidReader/              <- der Sketch (rfidReader.ino + weitere .ino/.h Dateien)
```

### 2. Arduino IDE installieren

Download unter <https://www.arduino.cc/en/software/> und installieren.

### 3. ESP32-Boardpaket installieren

1. **File → Preferences** öffnen.
2. Unter **Additional boards manager URLs** folgende URL eintragen:

   ```txt
   https://espressif.github.io/arduino-esp32/package_esp32_index.json
   ```

3. **Tools → Board → Boards Manager** öffnen, nach `esp32` suchen und
   **esp32 by Espressif Systems** installieren.

Ausführliche Anleitung: <https://docs.espressif.com/projects/arduino-esp32/en/latest/installing.html>

### 4. Bibliotheken bereitstellen

Die Arduino IDE sucht Bibliotheken **ausschließlich** im Ordner `libraries` innerhalb der
_Sketchbook location_. Ein Ordner `libraries` direkt im Sketch-Ordner (`rfidReader/libraries`)
wird **nicht** gefunden.

1. **File → Preferences** öffnen und den Pfad unter **Sketchbook location** ablesen
   (Standard unter Windows: `C:\Users\<Name>\Documents\Arduino`).
2. Den **Inhalt** von `kassensystem/ArduinoIDE_libraries/` in `<Sketchbook location>/libraries/`
   kopieren. Danach muss es z. B. `<Sketchbook location>/libraries/RFID_MFRC522v2/src/MFRC522v2.h` geben.
3. Die Arduino IDE **komplett schließen und neu starten**, damit die Bibliotheken eingelesen werden.

Alternative: Sketchbook location auf `kassensystem/rfidReader_ArduinoIDE` setzen und
`ArduinoIDE_libraries` dorthin als `rfidReader_ArduinoIDE/libraries` kopieren.

Enthaltene Bibliotheken (Stand 0.5.2):

| Bibliothek                   | Version | Verwendet für                |
| ---------------------------- | ------- | ---------------------------- |
| RFID_MFRC522v2               | 2.0.6   | RFID-Leser                   |
| ArduinoJson                  | 7.4.1   | Kommunikation mit dem Server |
| ESP32Time                    | 2.0.6   | Uhrzeit                      |
| Adafruit GFX Library         | 1.12.1  | Grafik                       |
| Adafruit ILI9341             | 1.6.1   | TFT-Display                  |
| Adafruit ImageReader Library | 2.9.2   | BMP-Bilder von der SD-Karte  |
| Adafruit SPIFlash            | 5.1.1   | Abhängigkeit ImageReader     |
| Adafruit EPD                 | 4.5.6   | Abhängigkeit ImageReader     |
| Adafruit BusIO               | 1.17.0  | Abhängigkeit Adafruit-Libs   |
| SdFat - Adafruit Fork        | 2.2.54  | SD-Karte                     |
| RAK14014-FT6336U             | 1.0.1   | Touch-Controller             |

### 5. Zugangsdaten anlegen (`netcred.h`)

Die Datei `netcred.h` mit WLAN- und Serverdaten ist **nicht** im Repository enthalten und muss
im Sketch-Ordner `rfidReader/` angelegt werden:

```cpp
/*
  netcred = networkcredentials
  hier werden die WLan- und Server-Informationen hinterlegt
*/
const char* WiFiSsid   = "WLAN-NAME";
const char* WiFiPassWd = "WLAN-PASSWORT";

const uint port = 5000;          // Port des Servers (TimeMachine)
const char* ip  = "192.168.x.x"; // Servername oder IP-Adresse
```

**Wichtig:** Diese Datei niemals committen (sie enthält das WLAN-Passwort).

### 6. Hardware-Variante im Code wählen

Am Anfang von `rfidReader.ino`:

```cpp
//#define BREADBOARD          // ESP32 Dev Module
#define PCB                   // ESP32 C3 auf der Platine
#define RFIDREADERRIGHT       // RFID-Leser rechts vom Display
```

- **PCB** – Platinenversion 2024 mit ESP32-C3 (Standard).
- **BREADBOARD** – Aufbau auf dem Steckbrett mit normalem ESP32.
  Es darf immer nur **eines** von beiden aktiv sein.
- **RFIDREADERRIGHT** – legt die Ausrichtung des Displays fest. Auskommentieren, wenn der
  RFID-Leser links vom Display montiert ist.

Die zugehörigen Pins stehen in `pinoutdef.h`.

### 7. Board-Einstellungen

Unter **Tools**:

| Einstellung     | PCB                  | BREADBOARD         |
| --------------- | -------------------- | ------------------ |
| Board           | `ESP32C3 Dev Module` | `ESP32 Dev Module` |
| USB CDC On Boot | **Enabled**          | –                  |
| Port            | COM-Port des ESP32   | COM-Port des ESP32 |

Ohne _USB CDC On Boot: Enabled_ erscheint beim ESP32-C3 keine Ausgabe im Serial Monitor.

### 8. Kompilieren und Flashen

1. `rfidReader_ArduinoIDE/rfidReader/rfidReader.ino` öffnen (die übrigen Dateien
   werden automatisch als Tabs mitgeladen).
2. **Verify** (✓) zum Kompilieren, **Upload** (→) zum Flashen.
3. **Tools → Serial Monitor** mit **115200 Baud** öffnen. Beim Start werden die
   SW-Version und der Servername ausgegeben.

### 9. SD-Karte

Die Bilder für das Display (`rfid.bmp`, `gate.bmp`, `cash.bmp`, `motor.bmp`, `check.bmp`,
`error.bmp`, `wlgrnsm.bmp`, ...) werden von einer SD-Karte im Hauptverzeichnis gelesen.
Ohne SD-Karte meldet der Serial Monitor `SD begin() failed` und es werden keine Bilder angezeigt.

### Häufige Fehler

| Fehlermeldung                                               | Ursache / Lösung                                                                             |
| ----------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| `MFRC522v2.h: No such file or directory` (oder andere `.h`) | Bibliotheken liegen nicht in `<Sketchbook location>/libraries` → Schritt 4, IDE neu starten. |
| `netcred.h: No such file or directory`                      | Datei fehlt → Schritt 5.                                                                     |
| Keine Ausgabe im Serial Monitor                             | _USB CDC On Boot_ auf _Enabled_ stellen, Baudrate 115200.                                    |
| Display steht auf dem Kopf / falsche Ausrichtung            | `RFIDREADERRIGHT` umschalten.                                                                |
| `SD begin() failed`                                         | SD-Karte fehlt oder nicht FAT-formatiert.                                                    |
