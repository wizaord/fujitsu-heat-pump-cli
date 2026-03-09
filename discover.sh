#!/usr/bin/env bash
# =============================================================================
# discover.sh — Découverte de la pompe à chaleur Fujitsu AirStage sur le réseau
# =============================================================================
# Usage:
#   ./discover.sh                    # Auto-détecte le sous-réseau
#   ./discover.sh 192.168.1          # Scan le sous-réseau 192.168.1.x
#
# Le device ID est extrait du SSID AP-WJ3E-94BB43E75769 → 94BB43E75769
# Si l'IP est trouvée, elle est sauvegardée dans ~/.airstage.json
# =============================================================================

set -euo pipefail

DEVICE_ID="${AIRSTAGE_DEVICE_ID:-94BB43E75769}"
CONFIG_FILE="${HOME}/.airstage.json"
TIMEOUT=2
PARALLEL_JOBS=50

# --- Couleurs ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}=== Découverte AirStage Fujitsu ===${NC}"
echo "Device ID : ${DEVICE_ID}"

# --- Détection du sous-réseau ---
detect_subnet() {
    local subnet=""
    # macOS
    if command -v ipconfig &>/dev/null; then
        # Essaie les interfaces courantes
        for iface in en0 en1 en2; do
            subnet=$(ipconfig getifaddr "$iface" 2>/dev/null | sed 's/\.[0-9]*$//' || true)
            if [ -n "$subnet" ]; then
                echo "$subnet"
                return
            fi
        done
    fi
    # Linux
    if command -v ip &>/dev/null; then
        subnet=$(ip route | grep -E "^[0-9]" | grep -v "^169" | head -1 | awk '{print $1}' | sed 's/\.[0-9]*\/.*$//' || true)
        if [ -n "$subnet" ]; then
            echo "$subnet"
            return
        fi
    fi
    # Fallback
    echo "192.168.1"
}

SUBNET="${1:-$(detect_subnet)}"
echo "Sous-réseau : ${SUBNET}.1-254"
echo ""

# --- Requête GetParam minimale ---
build_request() {
    cat <<EOF
{"device_id":"${DEVICE_ID}","device_sub_id":0,"req_id":"","modified_by":"","set_level":"03","list":["iu_model","iu_onoff"]}
EOF
}

# --- Résultat partagé via fichier temporaire ---
RESULT_FILE=$(mktemp)
trap 'rm -f "$RESULT_FILE"' EXIT

# --- Fonction de test d'une IP ---
test_ip() {
    local ip="$1"
    local body
    body=$(build_request)

    # Tente HTTPS d'abord (port 443, firmware récent)
    local response
    response=$(curl -sk --max-time "${TIMEOUT}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$body" \
        "https://${ip}/GetParam" 2>/dev/null || true)

    local protocol="https"
    if [ -z "$response" ] || ! echo "$response" | grep -q '"result"'; then
        # Fallback HTTP (port 80, ancien firmware)
        response=$(curl -s --max-time "${TIMEOUT}" \
            -X POST \
            -H "Content-Type: application/json" \
            -d "$body" \
            "http://${ip}/GetParam" 2>/dev/null || true)
        protocol="http"
    fi

    if echo "$response" | grep -q '"result":"OK"'; then
        local model
        model=$(echo "$response" | sed 's/.*"iu_model":"\([^"]*\)".*/\1/' 2>/dev/null || echo "unknown")
        echo "${ip}|${protocol}|${model}" > "$RESULT_FILE"
        echo -e "${GREEN}[TROUVÉ]${NC} IP: ${ip} (${protocol}) — Modèle: ${model}"
    fi
}

export -f test_ip build_request
export DEVICE_ID TIMEOUT RESULT_FILE

# --- Scan parallèle ---
echo -e "${YELLOW}Scan en cours...${NC}"
seq 1 254 | xargs -P "${PARALLEL_JOBS}" -I{} bash -c "test_ip '${SUBNET}.{}'" 2>/dev/null

echo ""

# --- Lecture du résultat ---
if [ -s "$RESULT_FILE" ]; then
    IFS='|' read -r FOUND_IP FOUND_PROTOCOL FOUND_MODEL < "$RESULT_FILE"

    echo -e "${GREEN}Pompe à chaleur trouvée !${NC}"
    echo "  IP       : ${FOUND_IP}"
    echo "  Protocole: ${FOUND_PROTOCOL}"
    echo "  Modèle   : ${FOUND_MODEL}"
    echo "  Device ID: ${DEVICE_ID}"

    # Sauvegarde dans ~/.airstage.json
    USE_HTTPS="true"
    if [ "$FOUND_PROTOCOL" = "http" ]; then
        USE_HTTPS="false"
    fi

    cat > "$CONFIG_FILE" <<EOF
{
  "ip": "${FOUND_IP}",
  "deviceId": "${DEVICE_ID}",
  "useHttps": ${USE_HTTPS}
}
EOF
    echo ""
    echo -e "${CYAN}Configuration sauvegardée dans ${CONFIG_FILE}${NC}"
    echo ""
    echo "Vous pouvez maintenant utiliser le CLI :"
    echo "  ./gradlew run --args=\"status\""
    echo "  ./gradlew run --args=\"on\""
    echo "  ./gradlew run --args=\"off\""
else
    echo -e "${RED}Aucune pompe à chaleur trouvée sur le sous-réseau ${SUBNET}.x${NC}"
    echo ""
    echo "Vérifications possibles :"
    echo "  1. Confirmez que l'adaptateur WiFi est connecté au réseau"
    echo "  2. Vérifiez le sous-réseau : ./discover.sh 192.168.0"
    echo "  3. Vérifiez le Device ID dans le SSID de l'adaptateur (AP-WJ3E-XXXXXXXXXXXX)"
    echo "  4. L'adaptateur doit être visible dans l'app AirStage Mobile"
    exit 1
fi
