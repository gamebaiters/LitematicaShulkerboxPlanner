#!/usr/bin/env bash
# Builds the images in docs/images from the client game test screenshots.
#   1. ./gradlew runClientGameTest   (writes build/run/clientGameTest/screenshots)
#   2. bash tools/media/render.sh
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SHOTS="$ROOT/build/run/clientGameTest/screenshots"
MEDIA="$ROOT/tools/media"
OUT="$ROOT/docs/images"
EDGE="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"

mkdir -p "$OUT/screenshots"

pick() { ls "$SHOTS"/*_"$1".png | head -n 1; }

cp "$(pick 01_litematica_material_list)" "$OUT/screenshots/material-list-button.png"
cp "$(pick 02_shulker_boxes)"            "$OUT/screenshots/shulker-boxes.png"
cp "$(pick 03_materials)"                "$OUT/screenshots/materials.png"
cp "$(pick 04_reminders)"                "$OUT/screenshots/reminders.png"
cp "$(pick 05_inventory_created_boxes)"  "$OUT/screenshots/creative-boxes.png"
cp "$(pick 06_hud)"                      "$OUT/screenshots/hud.png"
cp "$(pick 06b_hud_shulker)"             "$OUT/screenshots/hud-shulker.png"
cp "$(pick 06c_hud_compact)"             "$OUT/screenshots/hud-compact.png"
cp "$(pick 11_hud_editor)"               "$OUT/screenshots/hud-editor.png"
cp "$(pick 07_boxes_progress)"           "$OUT/screenshots/progress.png"
cp "$(pick 08_packing_assistant)"        "$OUT/screenshots/packing-assistant.png"
cp "$(pick 09_litematica_main_menu)"     "$OUT/screenshots/main-menu.png"
cp "$(pick 10_config)"                   "$OUT/screenshots/config.png"

# Inputs for the html templates
cp "$OUT/screenshots/shulker-boxes.png"        "$MEDIA/shot_a.png"
cp "$OUT/screenshots/packing-assistant.png"    "$MEDIA/shot_b.png"
cp "$OUT/screenshots/packing-assistant.png"    "$MEDIA/shot_c.png"
cp "$OUT/screenshots/hud.png"                  "$MEDIA/shot_d.png"
cp "$OUT/screenshots/materials.png"            "$MEDIA/shot_e.png"

render() {
    local html="$1" png="$2" size="$3"
    "$EDGE" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
            --virtual-time-budget=8000 --window-size="$size" \
            --screenshot="$(cygpath -w "$png")" "file:///$(cygpath -m "$html")" >/dev/null 2>&1
    echo "rendered $png"
}

# Icons: 512 px for Modrinth / docs, 128 px inside the mod jar
render "$MEDIA/icon.html" "$OUT/icon.png" "512,512"
"$EDGE" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=0.25 \
        --virtual-time-budget=3000 --window-size=512,512 \
        --screenshot="$(cygpath -w "$ROOT/src/main/resources/assets/shulkerplanner/icon.png")" \
        "file:///$(cygpath -m "$MEDIA/icon.html")" >/dev/null 2>&1
echo "rendered mod icon (128 px)"
cp "$OUT/icon.png" "$MEDIA/icon.png"

render "$MEDIA/banner.html"   "$OUT/banner.png"   "1600,500"
render "$MEDIA/features.html" "$OUT/features.png" "1920,1080"

rm -f "$MEDIA"/shot_*.png "$MEDIA/icon.png"
