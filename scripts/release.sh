#!/usr/bin/env bash
set -e

# ==============================================================================
# SprachCafé Android Release CLI Tool
# Automatisierter Build-, Versions- und Bereitstellungs-Workflow
# ==============================================================================

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

MESSAGE="${1:-Release neuer App-Stand}"

echo "======================================================================"
echo "☕ SprachCafé Android App - Automatisierter Release-Workflow"
echo "======================================================================"
echo "📌 Arbeitsverzeichnis: $REPO_DIR"
echo "📝 Release-Notiz:     $MESSAGE"
echo "======================================================================"

# 1. GitHub Token ermitteln
GITHUB_TOKEN=$(grep github.com ~/.git-credentials 2>/dev/null | sed -e 's/.*:\(.*\)@.*/\1/' || true)
if [ -z "$GITHUB_TOKEN" ]; then
    echo "⚠️ Kein GitHub-Token in ~/.git-credentials gefunden. Versuche ohne Authentifizierung..."
fi

# 2. Git Status prüfen
echo "🔍 Prüfe lokalen Git-Status..."
git add -A
if git diff --cached --quiet; then
    echo "ℹ️ Keine ungespeicherten Codeänderungen vorhanden. Erzeuge Release-Trigger..."
    git commit --allow-empty -m "release: $MESSAGE"
else
    git commit -m "feat/release: $MESSAGE"
fi

echo "🚀 Pushe Änderungen auf 'main'..."
git push origin main

echo "⏳ Warte auf GitHub Actions Cloud-Build..."
sleep 5

# 3. GitHub Actions Run überwachen
if [ -n "$GITHUB_TOKEN" ]; then
    RUN_ID=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
        "https://api.github.com/repos/fuchstv/sprachcafe-android/actions/runs?per_page=1" \
        | jq -r '.workflow_runs[0].id // empty')
    
    if [ -n "$RUN_ID" ]; then
        echo "📡 GitHub Actions Run ID: $RUN_ID gestartet."
        echo -n "⚙️ Kompiliere Android Release AAB + APK in der Cloud"
        
        while true; do
            STATUS_JSON=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
                "https://api.github.com/repos/fuchstv/sprachcafe-android/actions/runs/$RUN_ID")
            STATUS=$(echo "$STATUS_JSON" | jq -r '.status')
            CONCLUSION=$(echo "$STATUS_JSON" | jq -r '.conclusion // "running"')
            
            if [ "$STATUS" == "completed" ]; then
                echo ""
                if [ "$CONCLUSION" == "success" ]; then
                    echo "✅ Cloud-Build erfolgreich abgeschlossen!"
                else
                    echo "❌ Cloud-Build fehlgeschlagen mit Status: $CONCLUSION"
                    echo "🔗 Details: https://github.com/fuchstv/sprachcafe-android/actions/runs/$RUN_ID"
                    exit 1
                fi
                break
            fi
            echo -n "."
            sleep 10
        done

        # 4. Neuestes Release abfragen und lokale Downloads aktualisieren
        echo "📥 Frage Release-Pakete von GitHub ab..."
        RELEASE_JSON=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
            "https://api.github.com/repos/fuchstv/sprachcafe-android/releases/latest")
        TAG_NAME=$(echo "$RELEASE_JSON" | jq -r '.tag_name')
        APK_URL=$(echo "$RELEASE_JSON" | jq -r '.assets[] | select(.name | endswith(".apk")) | .browser_download_url')
        AAB_URL=$(echo "$RELEASE_JSON" | jq -r '.assets[] | select(.name | endswith(".aab")) | .browser_download_url')

        echo "📦 Neuestes Release: $TAG_NAME"
        
        # Lokalen Server-Download aktualisieren falls Verzeichnis existiert
        if [ -d "/home/ubuntu/sprachcafe-team/public/downloads" ] && [ -n "$APK_URL" ]; then
            echo "🔄 Aktualisiere lokalen Download-Server..."
            curl -sL -H "Authorization: token $GITHUB_TOKEN" -H "Accept: application/octet-stream" \
                "$APK_URL" -o "/home/ubuntu/sprachcafe-team/public/downloads/app-release.apk"
            echo "✅ /home/ubuntu/sprachcafe-team/public/downloads/app-release.apk aktualisiert."
        fi

        mkdir -p "$REPO_DIR/build-releases"
        if [ -n "$AAB_URL" ]; then
            echo "📥 Lade signiertes AAB für Google Play herunter..."
            curl -sL -H "Authorization: token $GITHUB_TOKEN" -H "Accept: application/octet-stream" \
                "$AAB_URL" -o "$REPO_DIR/build-releases/app-release-latest.aab"
            echo "✅ Gespeichert unter: $REPO_DIR/build-releases/app-release-latest.aab"
        fi
    fi
fi

echo ""
echo "======================================================================"
echo "🎉 Release erfolgreich bereitgestellt!"
echo "======================================================================"
echo "📲 Direkt-Download APK: https://team.xn--sprachcaf-j4a.org/downloads/app-release.apk"
echo "🏢 Google Play AAB:     $REPO_DIR/build-releases/app-release-latest.aab"
echo ""
echo "👉 Managed Google Play Update (1 Klick):"
echo "   Öffne: https://admin.google.com/u/1/ac/devices/apps/org.sprachcafe.team"
echo "   Klicke auf 'Neu bearbeiten / Neue Version hochladen' und wähle 'app-release-latest.aab' aus."
echo "   (Oder richte den Service-Account-Key für 0-Klick Auto-Publish ein)."
echo "======================================================================"
