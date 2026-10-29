#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'Release version error: %s\n' "$*" >&2
  exit 1
}

parse_version() {
  local value="$1"
  if [[ ! "$value" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
    fail "Invalid version '$value'. Expected MAJOR.MINOR.PATCH without prerelease suffixes."
  fi
  major="${BASH_REMATCH[1]}"
  minor="${BASH_REMATCH[2]}"
  patch="${BASH_REMATCH[3]}"
}

calculate_version_code() {
  parse_version "$1"
  (( major <= 1999 && minor <= 999 && patch <= 999 )) ||
    fail "Version components exceed Android versionCode limits (major <= 1999, minor/patch <= 999)."
  version_code=$((10#$major * 1000000 + 10#$minor * 1000 + 10#$patch))
  (( version_code > 0 )) || fail "Android versionCode must be greater than zero."
}

if [[ "$GITHUB_EVENT_NAME" == "workflow_dispatch" ]]; then
  [[ "$GITHUB_REF" == "refs/heads/main" ]] ||
    fail "Manual releases must be run from the main branch."

  bump="${RELEASE_BUMP:-patch}"
  case "$bump" in
    patch|minor|major) ;;
    *) fail "Unsupported bump '$bump'." ;;
  esac

  # Only stable semver tags count. sort -V compares numeric components.
  latest_tag=$(git tag --list 'v*' |
    grep -E '^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$' |
    sort -V | tail -n 1 || true)

  requested="${RELEASE_VERSION:-}"
  if [[ -n "$requested" ]]; then
    version="${requested#v}"
  elif [[ -z "$latest_tag" ]]; then
    # The first release matches the current Android versionName.
    version="0.2.0"
  else
    parse_version "${latest_tag#v}"
    case "$bump" in
      patch) patch=$((patch + 1)) ;;
      minor) minor=$((minor + 1)); patch=0 ;;
      major) major=$((major + 1)); minor=0; patch=0 ;;
    esac
    version="$major.$minor.$patch"
  fi
  calculate_version_code "$version"
  next_code="$version_code"
  tag="v$version"

  if [[ -n "$latest_tag" ]]; then
    calculate_version_code "${latest_tag#v}"
    (( next_code > version_code )) ||
      fail "Version $version is not newer than latest release tag $latest_tag."
  fi

  if git show-ref --verify --quiet "refs/tags/$tag"; then
    fail "Tag $tag already exists; use a new version instead."
  fi
  version_code="$next_code"
elif [[ "$GITHUB_EVENT_NAME" == "push" && "$GITHUB_REF" == refs/tags/* ]]; then
  tag="$GITHUB_REF_NAME"
  [[ "$tag" == v* ]] || fail "Expected a v-prefixed tag."
  version="${tag#v}"
  calculate_version_code "$version"
else
  fail "Only manual runs from main or version-tag pushes may publish releases."
fi

{
  printf 'tag=%s\n' "$tag"
  printf 'version=%s\n' "$version"
  printf 'version_code=%s\n' "$version_code"
} >> "$GITHUB_OUTPUT"

printf 'Resolved release: %s (Android versionCode %s)\n' "$tag" "$version_code"
