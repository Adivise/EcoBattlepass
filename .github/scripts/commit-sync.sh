#!/usr/bin/env bash
set -euo pipefail
: "${GITHUB_OUTPUT:?}"
: "${DEFAULT_BRANCH:?}"
: "${HAS_UPDATE:?}"
: "${UPSTREAM_SHA:?}"

git add -A
if [ "$HAS_UPDATE" = "pointer" ]; then
  git commit -m "chore: advance upstream pointer (${UPSTREAM_SHA})" || { git status; exit 1; }
else
  git commit -m "chore: sync upstream (${UPSTREAM_SHA})" || { git status; exit 1; }
fi

git config push.followTags false
git -c push.followTags=false push origin "HEAD:refs/heads/${DEFAULT_BRANCH}" --no-tags

echo "head_sha=$(git rev-parse HEAD)" >> "$GITHUB_OUTPUT"
