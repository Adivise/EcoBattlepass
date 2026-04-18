#!/usr/bin/env bash
# Sync fork from upstream: only changed paths (not .github/ or README.md).
# Writes step outputs to GITHUB_OUTPUT (GitHub Actions).
set -euo pipefail

: "${GITHUB_OUTPUT:?}"

UPSTREAM_URL="${UPSTREAM_URL:-https://github.com/Auxilor/EcoBattlepass.git}"

git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git config user.name "github-actions[bot]"
git config push.followTags false
git remote add upstream "$UPSTREAM_URL" 2>/dev/null || true
git fetch origin --no-tags
git remote set-head origin -a 2>/dev/null || true
git fetch upstream --no-tags

if [ "${GITHUB_ACTIONS:-}" = "true" ]; then
  while IFS= read -r t; do
    [ -z "$t" ] && continue
    git tag -d "$t" 2>/dev/null || true
  done < <(git tag -l)
fi

DEFAULT_BRANCH=""
origin_head_ref="$(git symbolic-ref -q refs/remotes/origin/HEAD 2>/dev/null || true)"
if [ -n "$origin_head_ref" ]; then
  DEFAULT_BRANCH="${origin_head_ref#refs/remotes/origin/}"
fi
if [ -z "$DEFAULT_BRANCH" ]; then
  for cand in main master trunk; do
    if git rev-parse "refs/remotes/origin/${cand}" >/dev/null 2>&1; then
      DEFAULT_BRANCH="$cand"
      break
    fi
  done
fi
if [ -z "$DEFAULT_BRANCH" ]; then
  echo "Could not determine origin default branch (no origin/HEAD and no origin/main|master)."
  exit 1
fi
echo "default_branch=$DEFAULT_BRANCH" >> "$GITHUB_OUTPUT"

UPSTREAM_DEFAULT="$(git ls-remote --symref upstream HEAD | awk '/^ref:/ {sub(/^refs\/heads\//, "", $2); print $2; exit}')"
if [ -z "${UPSTREAM_DEFAULT}" ]; then
  echo "Could not detect upstream default branch."
  exit 1
fi
echo "upstream_default=$UPSTREAM_DEFAULT" >> "$GITHUB_OUTPUT"

NEW_SHA="$(git rev-parse "upstream/${UPSTREAM_DEFAULT}")"
echo "upstream_sha=$NEW_SHA" >> "$GITHUB_OUTPUT"

SYNC_CONTENT="$(git show "origin/${DEFAULT_BRANCH}:.upstream-sync" 2>/dev/null || true)"

OLD_SHA="$(printf '%s\n' "$SYNC_CONTENT" | grep '^UPSTREAM_SHA=' | head -1 | cut -d= -f2- | tr -d '\r' | tr -d ' ')" || OLD_SHA=""

OLD_VER="$(git show "origin/${DEFAULT_BRANCH}:gradle.properties" 2>/dev/null | grep -E '^version[[:space:]]*=' | head -1 | sed 's/^[^=]*=[[:space:]]*//;s/[[:space:]]*$//;s/#.*//' || true)"

if [ -n "$OLD_SHA" ] && [ "$OLD_SHA" = "$NEW_SHA" ]; then
  echo "Upstream unchanged ($NEW_SHA). Skipping."
  echo "has_update=false" >> "$GITHUB_OUTPUT"
  echo "run_build=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

if [ -n "$OLD_SHA" ] && ! git cat-file -e "${OLD_SHA}^{commit}" 2>/dev/null; then
  echo "Warning: stored UPSTREAM_SHA is missing from history; doing full tree sync from upstream."
  OLD_SHA=""
fi

FORK_SHA="$(git rev-parse "origin/${DEFAULT_BRANCH}")"
git checkout -B "$DEFAULT_BRANCH" "origin/${DEFAULT_BRANCH}"

DELTA_NONPROTECTED=0
if [ -z "$OLD_SHA" ]; then
  echo "No prior upstream pointer: syncing all upstream files (except .github and README.md)."
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in .github/*|.github|README.md) continue ;; esac
    DELTA_NONPROTECTED=$((DELTA_NONPROTECTED + 1))
    git checkout "$NEW_SHA" -- "$f"
  done < <(git ls-tree -r --name-only "$NEW_SHA")
else
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in .github/*|.github|README.md) continue ;; esac
    DELTA_NONPROTECTED=$((DELTA_NONPROTECTED + 1))
  done < <(git diff --name-only "$OLD_SHA" "$NEW_SHA")
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in .github/*|.github|README.md) continue ;; esac
    git rm -f --ignore-unmatch -- "$f" 2>/dev/null || true
  done < <(git diff --diff-filter=D --name-only "$OLD_SHA" "$NEW_SHA")
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in .github/*|.github|README.md) continue ;; esac
    git checkout "$NEW_SHA" -- "$f"
  done < <(git diff --diff-filter=d --name-only "$OLD_SHA" "$NEW_SHA")
fi

git checkout "$FORK_SHA" -- .github README.md

if [ "$DELTA_NONPROTECTED" -eq 0 ] && [ -n "$OLD_SHA" ]; then
  echo "Upstream moved but only .github/README (or skipped paths) changed; advancing pointer without build."
  echo "UPSTREAM_SHA=$NEW_SHA" > .upstream-sync
  echo "has_update=pointer" >> "$GITHUB_OUTPUT"
  echo "run_build=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

PV="$(grep -E '^version[[:space:]]*=' gradle.properties | head -1 | sed 's/^[^=]*=[[:space:]]*//;s/[[:space:]]*$//;s/#.*//')"
if [ -z "$PV" ]; then
  echo "Could not read version from gradle.properties."
  exit 1
fi
echo "gradle_version=$PV" >> "$GITHUB_OUTPUT"

tag_exists() {
  git ls-remote --tags origin "refs/tags/$1" 2>/dev/null | grep -q .
}

next_rebuild_suffix() {
  local pv="$1"
  local pfx="$2"
  local prefix="${pfx}${pv}-rebuild-"
  local max_r=0
  local _sha ref tag n
  while IFS=$'\t' read -r _sha ref; do
    [ -z "$ref" ] && continue
    tag="${ref#refs/tags/}"
    case "$tag" in
      "${prefix}"[0-9]*) n="${tag#"$prefix"}" ;;
      *) continue ;;
    esac
    case "$n" in ''|*[!0-9]*) continue ;; esac
    if [ "$n" -gt "$max_r" ]; then max_r="$n"; fi
  done < <(git ls-remote --tags origin 2>/dev/null | grep -F "refs/tags/${prefix}" || true)
  echo $((max_r + 1))
}

# Detect prefix preference
TAG_PREFIX="v"
if ! git ls-remote --tags upstream "refs/tags/v*" | grep -q .; then
  TAG_PREFIX=""
fi

if [ "$PV" != "$OLD_VER" ] || [ -z "$OLD_SHA" ]; then
  TAG_NAME="${TAG_PREFIX}${PV}"
  # Delete the tag from origin in case it's an inherited tag from the upstream fork.
  # This allows the first build of any version to be "clean".
  git push origin ":refs/tags/${TAG_NAME}" 2>/dev/null || true
  RELEASE_NAME="${PV} (Auto Build)"
  KIND="auto"
else
  NEXT_R="$(next_rebuild_suffix "$PV" "$TAG_PREFIX")"
  TAG_NAME="${TAG_PREFIX}${PV}-rebuild-${NEXT_R}"
  RELEASE_NAME="${PV} (Rebuild-${NEXT_R})"
  KIND="rebuild"
fi

echo "tag_name=$TAG_NAME" >> "$GITHUB_OUTPUT"
echo "release_name=$RELEASE_NAME" >> "$GITHUB_OUTPUT"
echo "build_kind=$KIND" >> "$GITHUB_OUTPUT"
echo "has_update=true" >> "$GITHUB_OUTPUT"
echo "run_build=true" >> "$GITHUB_OUTPUT"

echo "UPSTREAM_SHA=$NEW_SHA" > .upstream-sync
