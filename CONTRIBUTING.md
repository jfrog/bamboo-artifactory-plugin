# Contributing Code to Bamboo Artifactory Plugin
Before submitting a pull request, please make sure your code is covered by tests.

## Building the Code
The code is built with Maven and JDK 8.
To build the plugin, please follow these steps:
1. Clone the code from GitHub.
2. Build and create the Bamboo Artifactory plugin jar by running the following Maven command:
```shell script
mvn clean package
```
After the build finished, you'll find the `bamboo-artifactory-plugin-<version>.jar` file under *target* directory. 
This jar can be loaded into Bamboo. 

## Testing the Code
To run integration tests, the plugin uses the [Atlassian Wired tests]((https://developer.atlassian.com/server/framework/atlassian-sdk/run-wired-tests-with-the-plugin-test-console)) infrastructure. 
The tests [inject Bamboo variables](https://www.jfrog.com/confluence/display/JFROG/Bamboo+Artifactory+Plug-in#BambooArtifactoryPlug-in-OverridingPlanvaluesusingBambooVariables) to override Artifactory credentials and repositories. 

### Preconditions
1. [Install the Atlassian SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/install-the-atlassian-sdk-on-a-linux-or-mac-system/).
For example, to install with Homebrew run:
```shell script
brew tap atlassian/tap
brew install atlassian/tap/atlassian-plugin-sdk
```
2. Configure the following environment variables:
* ARTIFACTORY_URL
* ARTIFACTORY_USERNAME
* ARTIFACTORY_PASSWORD
* MAVEN_HOME
* GRADLE_HOME

### Running the integration tests
To run the integration tests, execute the following command:
```shell script
atlas-clean && atlas-integration-test
```

### Running a single test
#### Step 1: Start the Bamboo server
The integration tests store a Bamboo home instance in a zip file in [src/test/resources/bamboo-home.zip](src/test/resources/bamboo-home.zip).
The bamboo home is automatically loaded into the tests environment using `atlas-*` commands.
To start the Bamboo server with the tests configuration run the following command:
```shell script
atlas-debug
```
Running the above command will start a Tomcat server with the Bamboo CI server, and the Bamboo Artifactory plugin installed.
After the server is started, navigate to http://localhost:6990/bamboo. The credentials are `admin:admin`.

#### Step 2: Run a single test
The plugin's integration tests run the jobs under the *Integration Tests* project. 
To run a test, open the *Developer Toolbar* by clicking on the arrow in the lower left corner of your browser.
Click Toolbox > Plugin Test Console.
The test console appears. In this console you can run a specific test.

### Creating an integration test
#### Introduction
The integration test should include 2 parts:
1. A job in the tests Bamboo home
2. Java code in `src/test/java/it/org/jfrog/bamboo/<testname>Test.java`

| Tip: During the development of the test, you may change the Java code and start the server over and over again - make sure to not clean the code (using *atlas-clean*) because it will remove the changes you made to the temporary Bamboo Home environment.
| --- |

#### Steps to create an integration test
1. Create a new class extending IntegrationTestsBase, in [src/test/java/it/org/jfrog/bamboo](./src/test/java/it/org/jfrog/bamboo) - see current tests for reference.
2. Start the Bamboo server as instructed in the previous section. Make sure the new test appear in the *Test Console*.
3. Temporary, configure real Artifactory credentials in: http://localhost:6990/bamboo/admin/jfrogConfig.action
4. Create a new plan under *Integration Tests* project. Make sure the plan key is same as in the Java code. 
The plan must capture the build info, include environment variables and finalized by the Artifactory Publish Build Info task.
5. Run the test as instructed in the section above.
6. Once the test passed, restore dummy Artifactory credentials configured in the UI.
7. Stop the server by `CTRL+C`. 
8. Run [./scripts/createBambooHome.sh](./scripts/createBambooHome.sh) to update the tests Bamboo home zip.