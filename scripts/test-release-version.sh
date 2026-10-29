#!/usr/bin/env bash
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/resolve-release-version.sh"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cd "$tmp"
git init --quiet
git config user.name "CI test"
git config user.email "ci@example.invalid"
git commit --allow-empty --quiet -m init

assert_release() {
  local event="$1" ref="$2" bump="$3" specified="$4" expected="$5" expected_code="$6"
  : > "$tmp/output"
  GITHUB_EVENT_NAME="$event" GITHUB_REF="$ref" GITHUB_REF_NAME="${ref##*/}" \
    RELEASE_BUMP="$bump" RELEASE_VERSION="$specified" GITHUB_OUTPUT="$tmp/output" \
    bash "$script"
  grep -qx "tag=v$expected" "$tmp/output"
  grep -qx "version=$expected" "$tmp/output"
  grep -qx "version_code=$expected_code" "$tmp/output"
}

assert_failure() {
  local event="$1" ref="$2" bump="$3" specified="$4"
  : > "$tmp/output"
  if GITHUB_EVENT_NAME="$event" GITHUB_REF="$ref" GITHUB_REF_NAME="${ref##*/}" \
      RELEASE_BUMP="$bump" RELEASE_VERSION="$specified" GITHUB_OUTPUT="$tmp/output" \
      bash "$script" > "$tmp/error.log" 2>&1; then
    printf 'Expected resolver failure but it succeeded\n' >&2
    exit 1
  fi
}

assert_release workflow_dispatch refs/heads/main patch '' 0.2.0 2000
assert_release workflow_dispatch refs/heads/main patch 0.2.0 0.2.0 2000
git tag v0.2.0
assert_release workflow_dispatch refs/heads/main patch '' 0.2.1 2001
assert_release workflow_dispatch refs/heads/main minor '' 0.3.0 3000
assert_release workflow_dispatch refs/heads/main major '' 1.0.0 1000000
assert_release workflow_dispatch refs/heads/main patch v0.5.4 0.5.4 5004
assert_release push refs/tags/v0.2.0 patch '' 0.2.0 2000
assert_failure workflow_dispatch refs/heads/main patch 0.2.0
assert_failure workflow_dispatch refs/heads/main patch 0.0.0
assert_failure workflow_dispatch refs/heads/main patch invalid
assert_failure workflow_dispatch refs/heads/develop patch ''
assert_failure workflow_dispatch refs/heads/main patch 2000.0.0
assert_failure push refs/tags/v1.2.3-beta patch ''

printf 'Release version resolver checks passed.\n'
