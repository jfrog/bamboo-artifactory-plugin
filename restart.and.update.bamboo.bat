@echo off
setlocal

set BAMBOO_INSTALL_DIR="C:\Program Files\Bamboo"
set BAMBOO_HOME_DIR=%USERPROFILE%"\bamboo-home"

echo "Cleaning existing plugin and dependencies"
del %$BAMBOO_INSTALL_DIR%"/webapp/WEB-INF/lib/"bamboo-artifactory-plugin*.jar"

echo "Building new plugin"
call mvn clean && call mvn package

echo "Updating Bamboo"
copy "target\bamboo-artifactory-plugin-1.5.x-SNAPSHOT.jar" %BAMBOO_INSTALL_DIR%"\webapp\WEB-INF\lib\bamboo-artifactory-plugin-1.5.x-SNAPSHOT.jar"

set JAVA_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

echo "Starting Bamboo instance"
call %BAMBOO_INSTALL_DIR%\BambooConsole.bat

@endlocal
:end