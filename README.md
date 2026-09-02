# 📱 SprachCafé Polnisch e.V. – Team & Betriebs-App (Android BYOD)

Moderne, native Android-Anwendung für das ehrenamtliche Team und die Schichtleitungen des **SprachCafé Polnisch e.V.** in Berlin-Pankow (Schulzestr. 1).

Entwickelt mit **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX** und **Google ML Kit** (100 % On-Device, 0 € Cloud-Kosten).

---

## 🌟 Kernfunktionen (MVP Phase 1)

1. **☕ Kiosk & Café Kasse:**
   - Touch-optimierte Kacheln für alle 18 Kiosk-, Getränke- und Ladenartikel (Mate, Ostmost, Kaffeesorten, Kuchen, Krówki, T-Shirts, Speak-Dating Merch).
   - Schnelle Mengenerfassung, flexibler Warenkorb und Sofort-Summenberechnung in EUR.
   - 1-Tap Verbuchung: *„💶 Bar bezahlt“* oder *„📝 Auf Strichliste buchen“*.
2. **📸 On-Device Barcode-Scanner (Google ML Kit):**
   - Live-Scan von EAN-13 Barcodes auf Getränkeflaschen und Snacks über die Handykamera.
   - Automatische Erkennung und Hinzufügung zum Warenkorb mit haptischem Feedback.
   - Funktioniert vollständig offline ohne externe API-Aufrufe.
3. **🧮 Digitaler Kassensturz & Tagesabschluss:**
   - Vorkonfigurierter **50,00 € Wechselgeldsockel**.
   - Schnelle Zählmaske für Scheine (50€ bis 5€) und Münzen (2€ bis 0,05€).
   - Automatische Ermittlung des Reinerlöses der Schicht und Erfassung von Barauslagen/Belegen.
4. **📋 GoBD-Minderungsprotokoll (Finanzamt-Schutz):**
   - Lückenlose Dokumentation von Schwund, Verderb, MHD-Ablauf und kostenloser Helferverpflegung.
   - Schützt die Gemeinnützigkeit und steuerliche Ordnungsmäßigkeit bei Prüfungen.
5. **📚 Hausbibliothek Scanner (400+ Bücher):**
   - Kamera-Scan von ISBN-10 und ISBN-13 Barcodes auf Buchrücken.
   - Schnelle Titel- und Autorenermittlung sowie Markierung von Ausleihe und Rückgabe.

---

## 🔒 BYOD Datenschutz & Sicherheit

- **Minimale Berechtigungen:** Nur `CAMERA` (für Barcodes) und `INTERNET` (für Synchronisation).
- **Keine privaten Daten:** Die App greift weder auf Kontakte, Fotos noch den Telefonspeicher zu.
- **Offline-First:** Verkäufe und Zählungen funktionieren auch bei Verbindungsproblemen im Café-Keller.

---

## 🚀 Build & CI/CD

Die App wird automatisch per **GitHub Actions** kompiliert und signiert:
- **Workflow:** `.github/workflows/android-build.yml`
- **Keystore:** RSA-2048 Release Key (`keystore/release.keystore`)
- **Ziel:** `app/build/outputs/apk/release/app-release.apk`
