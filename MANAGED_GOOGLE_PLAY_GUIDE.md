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

1. Öffne auf dem Android-Smartphone die normale **Google Play Store** App.
2. Tippe oben rechts auf das Profilbild und stelle sicher, dass das Vereinskonto **`...@sprachcafe-polnisch.org`** ausgewählt ist.
3. Im Google Play Store erscheint nun ein neuer Tab:  
   👉 **„SprachCafé Polnisch e.V.“** (neben *„Für dich“* und *„Top-Charts“*).
4. Tippe auf **SprachCafé Team** ➔ **„Installieren“**.
5. Die App wird wie jede normale App aus dem Store installiert – **vollständig konform mit dem Erweiterten Schutz!**
