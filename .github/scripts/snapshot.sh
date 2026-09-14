#!/usr/bin/env bash
# Snapshot logic for bamboo-artifactory-plugin, extracted from
# .github/workflows/snapshot.yml so it can be run both as a CI step and
# directly on a developer's machine (export the env vars below, then run
# this script from the repository root, with `jf` already configured
# against Artifactory - e.g. via `jf c add`).
#
# Expected environment variables:
#   CI                        - used by the JFrog CLI (set to true in CI)
#   JFROG_BUILD_STATUS        - build status reported to Artifactory build-info
#   JFROG_CLI_BUILD_NAME      - build-info build name
#   JFROG_CLI_BUILD_NUMBER    - build-info build number
#   JFROG_CLI_BUILD_PROJECT   - build-info project key
#   GITHUB_RUN_NUMBER         - used as the distributed release bundle version
#                               (set automatically by GitHub Actions; export
#                               a value manually when running locally)
set -euo pipefail

# Configure JFrog CLI Maven config
jf mvnc \
  --repo-resolve-releases ecosys-bamboo-repos --repo-resolve-snapshots ecosys-releases-snapshots \
  --repo-deploy-snapshots ecosys-oss-snapshot-local --repo-deploy-releases ecosys-oss-release-local

# Run audit
jf audit

# Delete previous snapshots to make sure the release bundle will not contain
# stale artifacts
jf rt del "ecosys-oss-snapshot-local/org/jfrog/bamboo/bamboo-artifactory-plugin/*" --quiet

# Build and publish
jf mvn clean install -U -B javadoc:jar source:jar

# Publish build info
jf rt bag
jf rt bce
jf rt bp

# Distribute release bundle
jf ds rbc ecosystem-artifactory-bamboo-plugin-snapshot "$GITHUB_RUN_NUMBER" --spec=./ci/release/specs/dev-rbc-filespec.json --sign
jf ds rbd ecosystem-artifactory-bamboo-plugin-snapshot "$GITHUB_RUN_NUMBER" --site="releases.jfrog.io" --sync
