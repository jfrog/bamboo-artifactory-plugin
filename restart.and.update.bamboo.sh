#!/bin/sh

echo "Stopping Bamboo instance"
sh /opt/bamboo/current/bamboo.sh stop

echo "Cleaning existing plugin and dependencies"
rm /opt/bamboo/current/webapp/WEB-INF/lib/bamboo-artifactory-plugin*.jar
rm -rf ~/bamboo-home/xml-data/build-dir/org.jfrog.bamboo.bamboo-artifactory-plugin-*

echo "Building new plugin"
mvn clean && mvn package

echo "Updating Bamboo"
cp target/bamboo-artifactory-plugin-1.5.x-SNAPSHOT.jar /opt/bamboo/current/webapp/WEB-INF/lib/

echo "Starting Bamboo instance"
sh /opt/bamboo/current/bamboo.sh start