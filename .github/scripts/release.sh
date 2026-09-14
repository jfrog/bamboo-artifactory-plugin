#!/usr/bin/env bash
# Release logic for bamboo-artifactory-plugin, extracted from
# .github/workflows/release.yml so it can be run both as a CI step and
# directly on a developer's machine (export the env vars below, then run
# this script from the repository root, with `jf` already configured
# against Artifactory - e.g. via `jf c add`).
#
# Expected environment variables:
#   NEXT_VERSION              - version to release (e.g. 3.4.0)
#   NEXT_DEVELOPMENT_VERSION  - next development version (e.g. 3.4.x-SNAPSHOT)
#   CI                        - used by the JFrog CLI (set to true in CI)
#   JFROG_BUILD_STATUS        - build status reported to Artifactory build-info
#   JFROG_CLI_BUILD_NAME      - build-info build name
#   JFROG_CLI_BUILD_NUMBER    - build-info build number
#   JFROG_CLI_BUILD_PROJECT   - build-info project key
set -euo pipefail

# Configure git identity
git config user.name "jfrog-ecosystem-automation"
git config user.email "eco-system@jfrog.com"

# Checkout release branch
git fetch origin release
git checkout release

# Check variables
test -n "$NEXT_VERSION"
test -n "$NEXT_DEVELOPMENT_VERSION"

# Configure JFrog CLI Maven config
jf mvnc \
  --repo-resolve-releases ecosys-bamboo-repos --repo-resolve-snapshots ecosys-releases-snapshots \
  --repo-deploy-snapshots ecosys-oss-snapshot-local --repo-deploy-releases ecosys-oss-release-local

# Run audit
jf audit

# Sync changes with master
git merge origin/master

# Update version
jf mvn versions:set -DnewVersion="${NEXT_VERSION}" -B

# Commit and tag release version
git commit -am "[artifactory-release] Release version ${NEXT_VERSION} [skipRun]" --allow-empty
git tag "${NEXT_VERSION}"

# Push release commit and tag
git push origin release
git push origin --tags

# Build and publish
jf mvn clean install -U -B

# Publish build info
jf rt bag
jf rt bce
jf rt bp

# Distribute release bundle
jf ds rbc ecosystem-artifactory-bamboo-plugin "$NEXT_VERSION" --spec=./ci/release/specs/prod-rbc-filespec.json --spec-vars="version=$NEXT_VERSION" --sign
jf ds rbd ecosystem-artifactory-bamboo-plugin "$NEXT_VERSION" --site="releases.jfrog.io" --sync

# Merge release into master
git checkout master
git merge origin/release

# Update next development version
jf mvn versions:set -DnewVersion="$NEXT_DEVELOPMENT_VERSION" -B

# Clean build artifacts (e.g. target/) left over from the release build above,
# so they aren't picked up by the next-development-version commit.
git clean -fd

# Commit next development version
git commit -am "[artifactory-release] Next development version [skipRun]"
git push origin master
