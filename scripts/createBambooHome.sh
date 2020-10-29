rm -rf target/bamboo/home/artifacts
rm -rf target/bamboo/home/temp
rm -rf target/bamboo/home/xml-data/builds
rm -rf target/bamboo/home/xml-data/build-dir
atlas-create-home-zip &&
cp target/bamboo/generated-test-resources.zip src/test/resources/bamboo-home.zip