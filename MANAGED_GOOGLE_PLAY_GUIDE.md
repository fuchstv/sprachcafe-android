# 📱 Anleitung: SprachCafé Team-App in Managed Google Play (Google Workspace)

Diese Anleitung erklärt, wie die Android-App **SprachCafé Team** als **private Unternehmens-App** in Google Workspace veröffentlicht wird.

---

## 🛡️ Warum dieser Weg für das „Erweiterte Sicherheitsprogramm“ nötig ist

* **Das Problem:** Nutzer im Google *Erweiterten Sicherheitsprogramm (Advanced Protection)* oder mit aktivem Google Play Protect Enhanced Security haben ein striktes **Sideloading-Verbot**. Das manuelle Installieren von `.apk`-Dateien aus dem Browser oder Downloads wird vom Betriebssystem aus Sicherheitsgründen blockiert.
* **Die Lösung:** Durch die Veröffentlichung als **Private App in Managed Google Play**:
  1. Die App wird offiziell über die **Google Play Store App** auf dem Smartphone verteilt.
  2. Google stuft sie als kryptografisch verifizierte, sichere App ein ➔ **Keine Blockade durch das Erweiterte Sicherheitsprogramm!**
  3. **0,00 € Kosten:** Es wird kein kostenpflichtiges Entwicklerkonto (25 $) benötigt.
  4. **100 % Privat:** Die App ist für die Öffentlichkeit unsichtbar und steht exklusiv Konten von `@sprachcafe-polnisch.org` zur Verfügung.
  5. **Automatische Updates:** Bei neuen Builds aktualisiert der Play Store die App automatisch im Hintergrund.

---

## 🚀 Schritt-für-Schritt: Veröffentlichung in der Google Workspace Admin Console

### Schritt 1: Das signierte `.aab`-Paket herunterladen
1. Öffne das neueste Release auf GitHub:  
   👉 [https://github.com/fuchstv/sprachcafe-android/releases](https://github.com/fuchstv/sprachcafe-android/releases)
2. Lade unter den Assets die Datei **`app-release.aab`** *(Android App Bundle)* herunter.
3. Lade bei Bedarf das fertige Store-Icon herunter:  
   👉 `play-store/icon-512.png` (im Repository).

---

### Schritt 2: Private App im Google Workspace hinterlegen
1. Öffne die **Google Workspace Admin Console**: 👉 [https://admin.google.com](https://admin.google.com) (mit deinem Admin-Konto `p.fuchs@sprachcafe-polnisch.org`).
2. Navigiere im linken Menü zu:  
   **Apps** ➔ **Web- und mobile Apps** (oder *Geräte* ➔ *Mobile Apps und Endgeräte* ➔ *Apps*).
3. Klicke auf das Dropdown **„App hinzufügen“** und wähle:  
   👉 **„Private Android-App hinzufügen“** *(oder „Über Google Play suchen“ ➔ Schlosssymbol „Private Apps“)*.
4. Klicke unten rechts auf das runde **„+“-Symbol** (Neue private App erstellen).
5. Gib die App-Details ein:
   * **Titel:** `SprachCafé Team`
   * **App-Paket hochladen:** Wähle die heruntergeladene Datei `app-release.aab` aus.
   * *(Optional) Symbol:* Lade das 512x512 Logo `play-store/icon-512.png` hoch.
6. Klicke auf **„Erstellen“** (Create).
7. Google verarbeitet das Bundle nun im Hintergrund. *(Hinweis: Dies dauert beim ersten Mal ca. 10–20 Minuten).*

---

### Schritt 3: Zugriff für das Team freischalten
1. Sobald die App in der Admin Console erscheint, klicke auf ihren Namen.
2. Gehe auf den Reiter **„Nutzerzugriff“** (User access).
3. Wähle:
   * **„Für alle aktiviert“** (oder wähle die Organisationseinheit/Gruppe für Schichtleiter & Ehrenamtliche).
4. *(Empfohlen)* Unter **„Verteilungsmethode“**:
   * Wähle **„Verfügbar“** (Available) ➔ Nutzer können die App selbst aus dem Play Store installieren.
   * *(Oder „Automatisch installieren“, falls alle Diensthandys die App sofort ohne Zutun erhalten sollen).*
5. Klicke auf **„Speichern“**.

---

## 📲 So installieren Teammitglieder die App auf ihrem Handy

1. **Arbeitsprofil (BYOD):** Wenn das Google-Workspace-Konto `@sprachcafe-polnisch.org` auf dem Android-Smartphone hinzugefügt wird, richtet Android automatisch das geschützte **Arbeitsprofil** ein.
2. Im App-Drawer gibt es den Reiter **„Arbeit“** (Apps mit dem kleinen blauen **Koffer-Symbol 💼**).
3. Öffne dort die App **Google Play Store (mit Koffer-Symbol 💼)**.
4. Im Store werden exklusiv die freigegebenen Vereins-Apps angezeigt.
5. Tippe auf **SprachCafé Team** ➔ **„Installieren“**.
6. Die App wird wie jede normale App aus dem Store installiert – **vollständig konform mit dem Erweiterten Sicherheitsprogramm!**

---

## 🔄 App aktualisieren mit dem CLI-Workflow (Option A)

Wenn neue Funktionen oder Fehlerbehebungen für die App programmiert wurden:

### 1. Build & Release per CLI auslösen:
```bash
cd /home/ubuntu/sprachcafe-android
./scripts/release.sh "Beschreibung des Updates (z. B. Kassen-Optimierung)"
```

Das Skript erledigt vollautomatisch:
* Git commit & push auf `main`
* Cloud-Build in GitHub Actions (Kompilierung mit JDK 17 & SDK 34)
* Erhöhung des `versionCode` für Google Play
* Erstellung des GitHub Releases
* Aktualisierung der Direkt-APK unter `https://team.sprachcafe-polnisch.org/downloads/app-release.apk`
* Herunterladen des signierten AAB-Pakets nach:  
  `build-releases/app-release-latest.aab`

### 2. AAB im Managed Google Play iFrame aktualisieren (10 Sekunden):
1. Öffne die Admin Console: [admin.google.com](https://admin.google.com) ➔ **Apps** ➔ **Web- und mobile Apps**.
2. Klicke auf **„App hinzufügen“** ➔ **„Private Android-App hinzufügen“** (das Schloss-Symbol 🔒).
3. Klicke im iFrame links auf das Schloss-Symbol **„Private Apps“**.
4. Wähle **„SprachCafé Team“** aus und klicke auf **„Bearbeiten“** (Stift-Symbol).
5. Ziehe die Datei `build-releases/app-release-latest.aab` hinein und klicke auf **Speichern**.

### 3. Automatische Verteilung an das Team:
* **Hintergrund-Update:** Google Play aktualisiert die App auf allen Geräten der Helfer automatisch im Hintergrund (über Nacht / im WLAN).
* **In-App Update:** Öffnet ein Teammitglied die App, prüft die integrierte Google Play Update API automatisch auf Aktualisierungen und bietet direkt den Button *„Jetzt aktualisieren“* an.
