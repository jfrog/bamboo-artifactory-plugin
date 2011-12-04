#!/bin/sh

if [ -z "$BAMBOO_INSTALL_DIR" ]; then
    BAMBOO_INSTALL_DIR="/opt/bamboo/current"
fi

if [ -z "$BAMBOO_HOME_DIR" ]; then
    BAMBOO_HOME_DIR="$HOME/bamboo-home"
fi

echo "Stopping Bamboo instance"
sh $BAMBOO_INSTALL_DIR/bamboo.sh stop

echo "Cleaning existing plugin and dependencies"
rm "$BAMBOO_INSTALL_DIR/webapp/WEB-INF/lib/"bamboo-artifactory-plugin*.jar
find "$BAMBOO_HOME_DIR/xml-data/build-dir" -name org.jfrog.bamboo.bamboo-artifactory-plugin* | xargs rm -rf

echo "Building new plugin"
mvn clean && mvn package

echo "Updating Bamboo"
cp target/bamboo-artifactory-plugin-1.5.x-SNAPSHOT.jar "$BAMBOO_INSTALL_DIR/webapp/WEB-INF/lib/"

if [ "$1" = "debug" ]; then
    export BAMBOO_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
else
    export BAMBOO_OPTIONS=""
fi

echo "Starting Bamboo instance"
sh $BAMBOO_INSTALL_DIR/bamboo.sh start