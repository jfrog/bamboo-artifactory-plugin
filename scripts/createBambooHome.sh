#!/bin/bash

# This script saves the Bamboo home environment started by `atlas-debug` or `atlas-debug` commands, used in the integration tests.
# The Bamboo home environment is saved in bamboo-home.zip file.
# Run this script after making changes in the Bamboo instance. For example - after adding a new test.

rm -rf target/bamboo/home/artifacts
rm -rf target/bamboo/home/temp
rm -rf target/bamboo/home/xml-data/builds
rm -rf target/bamboo/home/xml-data/build-dir
atlas-create-home-zip &&
  cp target/bamboo/generated-test-resources.zip src/test/resources/bamboo-home.zip
