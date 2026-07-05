#!/usr/bin/env bash
# Regenerate docs/screens.png and docs/concept.png from the HTML sources.
# Needs Google Chrome. Run from anywhere: bash docs/src/render.sh
set -euo pipefail

CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
  --virtual-time-budget=3500 --screenshot="$DIR/../screens.png" \
  --window-size=1300,724 "file://$DIR/screens.html"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
  --virtual-time-budget=3500 --screenshot="$DIR/../concept.png" \
  --window-size=1180,422 "file://$DIR/concept.html"

echo "Rendered docs/screens.png and docs/concept.png"
