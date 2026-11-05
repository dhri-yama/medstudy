#!/usr/bin/env bash
# Usage: tools/bump-version.sh major|minor|patch   (or: tools/bump-version.sh 1.2.3)
# Updates version.properties, turns the "Unreleased" section of CHANGELOG.md into the new version, commits and tags v<version>.
# Pushing is left to you: git push && git push --tags
set -euo pipefail
cd "$(dirname "$0")/.."
[ -z "$(git status --porcelain --untracked-files=no)" ] || { echo "Commit or stash your changes first."; exit 1; }
current=$(grep '^VERSION_NAME=' version.properties | cut -d= -f2)
IFS=. read -r major minor patch <<<"${current%%-*}"
case "${1:-}" in
  major) new="$((major + 1)).0.0" ;;
  minor) new="$major.$((minor + 1)).0" ;;
  patch) new="$major.$minor.$((patch + 1))" ;;
  [0-9]*.[0-9]*.[0-9]*) new="$1" ;;
  *) echo "Usage: $0 major|minor|patch|X.Y.Z"; exit 1 ;;
esac
sed -i.bak "s/^VERSION_NAME=.*/VERSION_NAME=$new/" version.properties && rm version.properties.bak
today=$(date +%Y-%m-%d)
python3 - "$new" "$today" <<'PY'
import sys, re
new, today = sys.argv[1:]
s = open("CHANGELOG.md").read()
s = s.replace("## Unreleased\n", f"## Unreleased\n\n(nothing yet)\n\n## {new} - {today}\n", 1)
open("CHANGELOG.md", "w").write(s)
PY
git add version.properties CHANGELOG.md
git commit -m "release: v$new"
git tag -a "v$new" -m "MedStudy $new"
echo "Released v$new (versionCode $((${new%%.*} * 10000 + $(echo $new | cut -d. -f2) * 100 + $(echo $new | cut -d. -f3))). Push with: git push && git push --tags"
